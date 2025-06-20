package com.entangle.analysis.handler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.entangle.analysis.entity.ImageDivisionInfo;
import com.entangle.analysis.entity.ServiceInfo;
import com.entangle.analysis.repository.ImageDivisionInfoRepository;
import com.entangle.analysis.service.AccessKeyService;
import com.entangle.analysis.service.ServiceInfoService;
import com.entangle.analysis.util.FileSizeFormatUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import analysis.AnalysisServiceImageDivOuterClass.EmbedDataType;
import analysis.AnalysisServiceImageDivOuterClass.ImageDivEmbedRequest;
import analysis.AnalysisServiceImageDivOuterClass.ImageDivEmbedResponse;
import analysis.AnalysisServiceImageDivOuterClass.ImageDivRestoreRequest;
import analysis.AnalysisServiceImageDivOuterClass.ImageDivRestoreResponse;
import io.grpc.stub.StreamObserver;

/**
 * 画像分割と埋め込みを行うgRPCサービスのハンドラークラス
 */
public class ImageDivHandler {
    private static final Logger log = LoggerFactory.getLogger(ImageDivHandler.class);
    private static final String PNG_UID_KEYWORD = "UID";
    private final MessageSource messageSource;
    private final Locale locale;
    private final AccessKeyService accessKeyService;
    private final ServiceInfoService serviceInfoService;
    private final ImageDivisionInfoRepository imageDivisionInfoRepository;
    private final String encryptKey;
    private final int defaultMaxUploadSize;

    /**
     * コンストラクタ
     * @param messageSource メッセージソース
     * @param locale ロケール
     * @param accessKeyService アクセスキーサービス
     * @param serviceInfoService サービス情報サービス
     * @param imageDivisionInfoRepository 画像分割情報リポジトリ
     * @param encryptKey 暗号化キー
     * @param defaultMaxUploadSize デフォルトの最大アップロードサイズ（バイト単位）
     */
    @Autowired
    public ImageDivHandler(MessageSource messageSource, Locale locale,
                        AccessKeyService accessKeyService, ServiceInfoService serviceInfoService,
                        ImageDivisionInfoRepository imageDivisionInfoRepository,
                        String encryptKey, int defaultMaxUploadSize) {
        this.messageSource = messageSource;
        this.locale = locale;
        this.accessKeyService = accessKeyService;
        this.serviceInfoService = serviceInfoService;
        this.imageDivisionInfoRepository = imageDivisionInfoRepository;
        this.encryptKey = encryptKey;
        this.defaultMaxUploadSize = defaultMaxUploadSize;
    }

    /**
     * 画像を分割し、UIDを埋め込んだ画像を生成するメソッド
     * @param request 画像分割と埋め込みリクエスト
     * @param responseObserver レスポンスオブザーバー
     */
    public void divideAndEmbed(ImageDivEmbedRequest request,
                            StreamObserver<ImageDivEmbedResponse> responseObserver) {
        String accessKey = request.getAccessKey();
        if (accessKey == null || !accessKeyService.isValid(accessKey)) {
            String msg = messageSource.getMessage("error.accesskey.invalid",
                                    null, locale);
            responseObserver.onError(io.grpc.Status.INTERNAL
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        String analysisName = request.getAnalysisName();
        ServiceInfo serviceInfo = serviceInfoService.findAll().stream()
            .filter(svcInfo -> analysisName != null &&
                                    analysisName.equals(svcInfo.getAnalysisName()))
            .findFirst().orElse(null);
        if (serviceInfo == null) {
            log.warn("ServiceInfoが見つかりません: analysisName={}", analysisName);
            String msg = messageSource.getMessage("unsupported.analysisName",
                                        new Object[]{analysisName}, locale);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        int maxUploadSize = defaultMaxUploadSize;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
            if (node.has("max_upload_size")) {
                maxUploadSize = node.get("max_upload_size").asInt(defaultMaxUploadSize);
            }
        } catch (Exception e) {
            log.warn("max_upload_size取得時のJSONパースエラー", e);
        }
        byte[] imageBytes = null;
        try {
            imageBytes = Base64.getDecoder().decode(request.getOriginalImageBase64());
        } catch (Exception e) {
            log.error("Base64デコード失敗", e);
            String msg = messageSource.getMessage("error.image.decode", null, locale);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        if (imageBytes.length > maxUploadSize) {
            log.warn("アップロード画像サイズ超過: アップロード値 > 制限値",
                    imageBytes.length, maxUploadSize);
            String msg = messageSource.getMessage("error.upload.size",
                                        new Object[]{FileSizeFormatUtil.formatBytes(
                                                        maxUploadSize, "MB")}, locale);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        int divisionNum = 0;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
            log.info("解析名: {} のServiceInfo JSON: {}", analysisName,
                        serviceInfo.getDataProcessInfoJson());
            if (node.has("divisionNum")) {
                divisionNum = node.get("divisionNum").asInt(divisionNum);
                log.info("divisionNum取得: {}", divisionNum);
            } else {
                log.warn("divisionNumがJSONに存在しません");
            }
        } catch (Exception e) {
            log.error("divisionNum取得時のJSONパースエラー", e);
        }
        if (divisionNum <= 1) {
            log.error("divisionNumが不正です: {}", divisionNum);
            String msg = messageSource.getMessage("error.division.num",
                                        new Object[]{divisionNum}, locale);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        List<String> dividedImages = new ArrayList<>();
        try {
            // Base64デコード→OpenCV Mat
            Mat img = opencv_imgcodecs.imdecode(new Mat(imageBytes), opencv_imgcodecs.IMREAD_COLOR);
            int width = img.cols();
            int height = img.rows();
            // divisionNumにできるだけ近い縦横グリッドを計算
            int gridRows = (int)Math.sqrt(divisionNum);
            int gridCols = (int)Math.ceil((double)divisionNum / gridRows);
            int cellW = width / gridCols;
            int cellH = height / gridRows;
            int count = 0;
            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++) {
                    if (count >= divisionNum) break;
                    int x = c * cellW;
                    int y = r * cellH;
                    int w = (c == gridCols - 1) ? (width - x) : cellW;
                    int h = (r == gridRows - 1) ? (height - y) : cellH;
                    Mat sub = new Mat(img, new Rect(x, y, w, h));
                    // Mat→Base64
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BufferedImage bimg = matToBufferedImage(sub);
                    ImageIO.write(bimg, "png", baos);
                    String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                    dividedImages.add(base64);
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("画像分割失敗", e);
            String msg = messageSource.getMessage("error.image.division",
                                        new Object[]{}, locale);
            responseObserver.onError(io.grpc.Status.INTERNAL
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        // --- ここから分割データ付与・UID生成・DB登録 ---
        EmbedDataType embedDataType = request.getEmbedDataType();
        String embedData = request.getEmbedData();
        divisionNum = dividedImages.size();
        String uid = UUID.randomUUID().toString();
        List<String> dividedImagesWithData = new ArrayList<>();
        for (int i = 0; i < dividedImages.size(); i++) {
            String base64 = dividedImages.get(i);
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(
                                                Base64.getDecoder().decode(base64)));
                // バイナリ方式でtEXtチャンクにUIDを書き込む
                String newBase64 = writeUidToPngBase64BinaryEdit(img, uid);
                dividedImagesWithData.add(newBase64);
            } catch (Exception e) {
                log.error("UID埋め込み失敗: part={}", i, e);
                String msg = messageSource.getMessage("error.uid.embed",
                                            new Object[]{i}, locale);
                responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(msg)
                    .asRuntimeException());
                return;
            }
        }
        ImageDivisionInfo info = new ImageDivisionInfo();
        info.setUid(uid);
        info.setDivisionNum(divisionNum);
        boolean encrypt = false;
        if (serviceInfo != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
                if (node.has("encryption")) {
                    encrypt = node.get("encryption").asBoolean(false);
                }
            } catch (Exception e) {
                log.warn("encryptionフラグ取得失敗", e);
            }
        }
        if (embedDataType == EmbedDataType.IMAGE) {
            if (encrypt && embedData != null && !embedData.isEmpty()) {
                try {
                    info.setEmbedMetaBase64(encrypt(embedData));
                } catch (Exception e) {
                    log.error("暗号化失敗", e);
                    String msg = messageSource.getMessage("error.encrypt",
                                                new Object[]{e.getMessage()}, locale);
                    responseObserver.onError(io.grpc.Status.INTERNAL
                        .withDescription(msg)
                        .asRuntimeException());
                    return;
                }
            } else {
                info.setEmbedMetaBase64(embedData != null ? embedData : "");
            }
            info.setEmbedMetaText(null);
        } else {
            if (encrypt && embedData != null && !embedData.isEmpty()) {
                try {
                    info.setEmbedMetaText(encrypt(embedData));
                } catch (Exception e) {
                    log.error("暗号化失敗", e);
                    String msg = messageSource.getMessage("error.encrypt",
                                                new Object[]{e.getMessage()}, locale);
                    responseObserver.onError(io.grpc.Status.INTERNAL
                        .withDescription(msg)
                        .asRuntimeException());
                    return;
                }
            } else {
                info.setEmbedMetaText(embedData != null ? embedData : "");
            }
            info.setEmbedMetaBase64(null);
        }
        imageDivisionInfoRepository.save(info);
        // --- 分割画像をtempDirPath/日時/に保存 ---
        String tempDirPath = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
            if (node.has("tempDirPath")) {
                tempDirPath = node.get("tempDirPath").asText();
            }
        } catch (Exception e) {
            log.warn("tempDirPath取得失敗", e);
        }
        if (tempDirPath != null && !dividedImages.isEmpty()) {
            String timeDir = LocalDateTime.now().format(
                                        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
            java.nio.file.Path outDir = java.nio.file.Paths.get(tempDirPath, timeDir);
            try {
                java.nio.file.Files.createDirectories(outDir);
                for (int i = 0; i < dividedImagesWithData.size(); i++) {
                    byte[] imgBytes = Base64.getDecoder().decode(dividedImagesWithData.get(i));
                    java.nio.file.Path outFile = outDir.resolve("part_" + i + ".png");
                    java.nio.file.Files.write(outFile, imgBytes);
                }
            } catch (Exception e) {
                log.warn("分割画像保存失敗: {}", outDir, e);
            }
        }
        // レスポンス
        ImageDivEmbedResponse response = ImageDivEmbedResponse.newBuilder()
                .addAllDividedImages(dividedImagesWithData)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * 埋め込まれたデータを復元するメソッド
     * @param request 復元リクエスト
     * @param responseObserver レスポンスオブザーバー
     */
    public void restoreEmbedData(ImageDivRestoreRequest request,
                                StreamObserver<ImageDivRestoreResponse> responseObserver) {
        String accessKey = request.getAccessKey();
        if (accessKey == null || !accessKeyService.isValid(accessKey)) {
            String msg = messageSource.getMessage("error.accesskey.invalid", null, locale);
            responseObserver.onError(io.grpc.Status.INTERNAL
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        String analysisName = request.getAnalysisName();
        ServiceInfo serviceInfo = serviceInfoService.findAll().stream()
            .filter(svcInfo -> analysisName != null &&
                                    analysisName.equals(svcInfo.getAnalysisName()))
            .findFirst().orElse(null);
        if (serviceInfo == null) {
            log.warn("ServiceInfoが見つかりません: analysisName={}", analysisName);
            String msg = messageSource.getMessage("unsupported.analysisName",
                                        new Object[]{analysisName}, locale);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        List<String> dividedImages = request.getDividedImagesList();
        if (dividedImages == null || dividedImages.isEmpty()) {
            String msg = messageSource.getMessage("error.image.notfound",
                                    null, locale);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        // --- PNG tEXtチャンクからUIDを抽出し、全画像で一致するか確認 ---
        List<String> extractedUids = new ArrayList<>();
        for (int i = 0; i < dividedImages.size(); i++) {
            String base64 = dividedImages.get(i);
            try {
                String extractedUid = extractUidFromPngBase64Binary(base64);
                if (extractedUid != null) {
                    extractedUids.add(extractedUid);
                } else {
                    extractedUids.add("");
                }
            } catch (Exception e) {
                extractedUids.add("");
            }
        }
        // 全てのUIDが同じかチェック
        String uid = null;
        boolean allSame = true;
        for (String u : extractedUids) {
            if (u == null || u.isEmpty()) {
                allSame = false;
                break;
            }
            if (uid == null) uid = u;
            if (!uid.equals(u)) {
                allSame = false;
                break;
            }
        }
        if (!allSame || uid == null) {
            String msg = messageSource.getMessage("error.uid.mismatch",
                                        new Object[]{extractedUids}, locale);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        ImageDivisionInfo info = imageDivisionInfoRepository.findByUid(uid);
        if (info == null) {
            String msg = messageSource.getMessage("error.divinfo.notfound",
                                        new Object[]{uid}, locale);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        if (info.getDivisionNum() != dividedImages.size()) {
            String msg = messageSource.getMessage("error.division.count.mismatch",
                                        new Object[]{info.getDivisionNum(),
                                                dividedImages.size()},locale);
            responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                .withDescription(msg)
                .asRuntimeException());
            return;
        }
        // embedDataTypeの判定（TEXT/IMAGE）
        String embedDataRestored = null;
        EmbedDataType embedDataTypeRestored = EmbedDataType.TEXT;
        if (info.getEmbedMetaBase64() != null && !info.getEmbedMetaBase64().isEmpty()) {
            embedDataRestored = info.getEmbedMetaBase64();
            embedDataTypeRestored = EmbedDataType.IMAGE;
        } else if (info.getEmbedMetaText() != null && !info.getEmbedMetaText().isEmpty()) {
            embedDataRestored = info.getEmbedMetaText();
            embedDataTypeRestored = EmbedDataType.TEXT;
        }
        // ServiceInfoのJSONからdivisionNumを取得し、DB・アップロード画像数と一致しているか確認
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
            int jsonDivisionNum = -1;
            if (node.has("divisionNum")) {
                jsonDivisionNum = node.get("divisionNum").asInt(-1);
            }
            if (jsonDivisionNum > 0) {
                if (jsonDivisionNum != info.getDivisionNum()
                    || jsonDivisionNum != dividedImages.size()) {
                    responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                        .withDescription(messageSource.getMessage("error.division.num.mismatch",
                                                        new Object[]{jsonDivisionNum,
                                                            info.getDivisionNum(),
                                                            dividedImages.size()}, locale))
                        .asRuntimeException());
                    return;
                }
            } else {
                log.warn("ServiceInfo JSONにdivisionNumが存在しません or 不正値");
            }
        } catch (Exception e) {
            log.error("ServiceInfo JSONからdivisionNum取得時のエラー", e);
        }
        // --- embedDataが暗号化されている場合は復号化 ---
        boolean encrypt = false;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
            if (node.has("encryption")) {
                encrypt = node.get("encryption").asBoolean(false);
            }
        } catch (Exception e) {
            log.warn("encryptionフラグ取得失敗", e);
        }
        if (encrypt && embedDataRestored != null && !embedDataRestored.isEmpty()) {
            try {
                embedDataRestored = decrypt(embedDataRestored);
            } catch (Exception e) {
                log.error("復号化失敗", e);
                responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(messageSource.getMessage("error.decrypt",
                                                    new Object[]{e.getMessage()}, locale))
                    .asRuntimeException());
                return;
            }
        }
        ImageDivRestoreResponse response = ImageDivRestoreResponse.newBuilder()
                .setEmbedData(embedDataRestored != null ? embedDataRestored : "")
                .setEmbedDataType(embedDataTypeRestored)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * OpenCV Mat→BufferedImage変換
     * @param mat 変換元のOpenCV Matオブジェクト
     * @return 変換後のBufferedImageオブジェクト
     */
    private BufferedImage matToBufferedImage(Mat mat) {
        // BGR→RGB変換
        Mat rgbMat = new Mat();
        if (mat.channels() == 3) {
            opencv_imgproc.cvtColor(mat, rgbMat, opencv_imgproc.COLOR_BGR2RGB);
        } else {
            rgbMat = mat.clone();
        }
        int type = (rgbMat.channels() == 1) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;
        int bufferSize = rgbMat.channels() * rgbMat.cols() * rgbMat.rows();
        byte[] b = new byte[bufferSize];
        rgbMat.data().get(b);
        BufferedImage image = new BufferedImage(rgbMat.cols(), rgbMat.rows(), type);
        image.getRaster().setDataElements(0, 0, rgbMat.cols(), rgbMat.rows(), b);
        return image;
    }

    /**
     * PNG画像にUIDをtEXtチャンクとして埋め込むメソッド
     * @param image 埋め込み対象のBufferedImage
     * @param uid 埋め込むUID
     * @return 埋め込み後のPNG画像をBase64エンコードした文字列
     */
    private String writeUidToPngBase64BinaryEdit(BufferedImage image, String uid) throws Exception {
        // まずImageIOでPNGバイナリを出力
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        // tEXtチャンクを作成
        String keyword = PNG_UID_KEYWORD;
        byte[] keywordBytes = keyword.getBytes("ISO-8859-1");
        byte[] valueBytes = uid.getBytes("ISO-8859-1");
        ByteArrayOutputStream chunkData = new ByteArrayOutputStream();
        chunkData.write(keywordBytes);
        chunkData.write(0); // null separator
        chunkData.write(valueBytes);
        byte[] textChunkData = chunkData.toByteArray();

        // チャンク長
        int dataLen = textChunkData.length;
        ByteArrayOutputStream chunk = new ByteArrayOutputStream();
        // 長さ（4バイト）
        chunk.write((dataLen >>> 24) & 0xFF);
        chunk.write((dataLen >>> 16) & 0xFF);
        chunk.write((dataLen >>> 8) & 0xFF);
        chunk.write(dataLen & 0xFF);
        // チャンクタイプ（tEXt）
        chunk.write('t'); chunk.write('E'); chunk.write('X'); chunk.write('t');
        // データ
        chunk.write(textChunkData);
        // CRC計算
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(new byte[]{'t','E','X','t'});
        crc.update(textChunkData);
        long crcValue = crc.getValue();
        chunk.write((int)((crcValue >>> 24) & 0xFF));
        chunk.write((int)((crcValue >>> 16) & 0xFF));
        chunk.write((int)((crcValue >>> 8) & 0xFF));
        chunk.write((int)(crcValue & 0xFF));
        byte[] textChunk = chunk.toByteArray();

        // PNGのIHDRチャンクの直後にtEXtチャンクを挿入
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // PNGヘッダ(8バイト)
        out.write(pngBytes, 0, 8);
        int pos = 8;
        // IHDRチャンク（必ず最初）
        int ihdrLen = 4 + 4 + 13 + 4; // 長さ+type+data+crc
        out.write(pngBytes, pos, ihdrLen);
        pos += ihdrLen;
        // tEXtチャンク
        out.write(textChunk);
        // 残りのチャンク
        out.write(pngBytes, pos, pngBytes.length - pos);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /**
     * PNG画像のBase64バイナリからUIDを抽出するメソッド
     * @param base64Png PNG画像のBase64エンコード文字列
     * @return 抽出したUID、または見つからなければnull
     */
    private String extractUidFromPngBase64Binary(String base64Png) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64Png);
        int pos = 8; // PNGヘッダ
        while (pos < bytes.length) {
            int len = ((bytes[pos] & 0xFF) << 24) | ((bytes[pos+1] & 0xFF) << 16)
                        | ((bytes[pos+2] & 0xFF) << 8) | (bytes[pos+3] & 0xFF);
            String type = new String(bytes, pos+4, 4, "ISO-8859-1");
            if ("tEXt".equals(type)) {
                // tEXtチャンクのデータ部をパース
                int dataStart = pos + 8;
                int dataEnd = dataStart + len;
                int sep = -1;
                for (int i = dataStart; i < dataEnd; i++) {
                    if (bytes[i] == 0) { sep = i; break; }
                }
                if (sep > 0) {
                    String keyword = new String(bytes, dataStart, sep - dataStart,
                                            "ISO-8859-1");
                    String value = new String(bytes, sep + 1, dataEnd - (sep + 1),
                                            "ISO-8859-1");
                    if (PNG_UID_KEYWORD.equals(keyword)) {
                        return value;
                    }
                }
            }
            pos += 8 + len + 4; // 長さ+type+data+CRC
        }
        return null;
    }

    /**
     * AES暗号化を行うメソッド
     * @param plainText 暗号化する平文
     * @return 暗号化されたBase64エンコード文字列
     */
    private String encrypt(String plainText) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] keyBytes = java.util.Arrays.copyOf(encryptKey.getBytes("UTF-8"), 16);
        javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = new byte[16];
        java.security.SecureRandom.getInstanceStrong().nextBytes(ivBytes);
        javax.crypto.spec.IvParameterSpec iv = new javax.crypto.spec.IvParameterSpec(ivBytes);
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, iv);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        byte[] result = new byte[ivBytes.length + encrypted.length];
        System.arraycopy(ivBytes, 0, result, 0, ivBytes.length);
        System.arraycopy(encrypted, 0, result, ivBytes.length, encrypted.length);
        return java.util.Base64.getEncoder().encodeToString(result);
    }

    /**
     * AES復号化を行うメソッド
     * @param cipherText 暗号化されたBase64エンコード文字列
     * @return 復号化された平文
     */
    private String decrypt(String cipherText) throws Exception {
        byte[] all = java.util.Base64.getDecoder().decode(cipherText);
        byte[] ivBytes = java.util.Arrays.copyOfRange(all, 0, 16);
        byte[] encBytes = java.util.Arrays.copyOfRange(all, 16, all.length);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] keyBytes = java.util.Arrays.copyOf(encryptKey.getBytes("UTF-8"), 16);
        javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        javax.crypto.spec.IvParameterSpec iv = new javax.crypto.spec.IvParameterSpec(ivBytes);
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, iv);
        byte[] decrypted = cipher.doFinal(encBytes);
        return new String(decrypted, "UTF-8");
    }
}
