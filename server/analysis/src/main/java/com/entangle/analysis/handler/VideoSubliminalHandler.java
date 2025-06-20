package com.entangle.analysis.handler;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.entangle.analysis.entity.ServiceInfo;
import com.entangle.analysis.service.AccessKeyService;
import com.entangle.analysis.service.ServiceInfoService;
import com.entangle.analysis.util.FileSizeFormatUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;

import analysis.AnalysisServiceVideoSubliminalOuterClass.SubliminalDetected;
import analysis.AnalysisServiceVideoSubliminalOuterClass.SubliminalImageInsert;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalCheckRequest;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalCheckResponse;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalGenerateRequest;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalGenerateResponse;

/**
 * 動画のサブリミナル生成とチェックを行う
 */
public class VideoSubliminalHandler {
    private static final Logger log = LoggerFactory.getLogger(VideoSubliminalHandler.class);
    private static final int PIXEL_DIFF_REGION_WIDTH = 100;
    private static final int PIXEL_DIFF_REGION_HEIGHT = 100;
    private final MessageSource messageSource;
    private final Locale locale;
    private final AccessKeyService accessKeyService;
    private final ServiceInfoService serviceInfoService;
    private final int defaultVideoMaxUploadSize;
    private final int defaultImageMaxUploadSize;
    private final double defaultSubliminalThreshold;
    private final int defaultSubliminalMaxDetected;

    /**
     * コンストラクタ
     * @param messageSource メッセージソース
     * @param locale ロケール
     * @param accessKeyService アクセスキーサービス
     * @param serviceInfoService サービス情報サービス
     * @param defaultVideoMaxUploadSize 動画の最大アップロードサイズ
     * @param defaultImageMaxUploadSize 画像の最大アップロードサイズ
     * @param defaultSubliminalThreshold サブリミナル検出の閾値
     * @param defaultSubliminalMaxDetected サブリミナル検出の最大数
     */
    @Autowired
    public VideoSubliminalHandler(MessageSource messageSource, Locale locale,
                            AccessKeyService accessKeyService, ServiceInfoService serviceInfoService,
                            int defaultVideoMaxUploadSize, int defaultImageMaxUploadSize,
                            double defaultSubliminalThreshold,
                            int defaultSubliminalMaxDetected) {
        this.messageSource = messageSource;
        this.locale = locale;
        this.accessKeyService = accessKeyService;
        this.serviceInfoService = serviceInfoService;
        this.defaultVideoMaxUploadSize = defaultVideoMaxUploadSize;
        this.defaultImageMaxUploadSize = defaultImageMaxUploadSize;
        this.defaultSubliminalThreshold = defaultSubliminalThreshold;
        this.defaultSubliminalMaxDetected = defaultSubliminalMaxDetected;
    }

    /**
     * 動画のサブリミナル生成を行う
     * @param request 生成リクエスト
     * @return 生成結果を含むレスポンス
     * @throws Exception 生成処理中のエラー
     */
    public VideoSubliminalGenerateResponse generateSubliminal(VideoSubliminalGenerateRequest request) {
        String accessKey = request.getAccessKey();
        if (accessKey == null || !accessKeyService.isValid(accessKey)) {
            String msg = messageSource.getMessage("error.accesskey.invalid",
                                        new Object[]{}, locale);
            return VideoSubliminalGenerateResponse.newBuilder().setMessage(msg).build();
        }
        String analysisName = request.getAnalysisName();
        ServiceInfo serviceInfo = serviceInfoService.findAll().stream()
            .filter(svcInfo -> analysisName != null && analysisName.equals(svcInfo.getAnalysisName()))
            .findFirst().orElse(null);
        if (serviceInfo == null) {
            String msg = messageSource.getMessage("unsupported.analysisName",
                                        new Object[]{analysisName}, locale);
            return VideoSubliminalGenerateResponse.newBuilder().setMessage(msg).build();
        }
        byte[] videoFile = request.getVideoFile().toByteArray();
        if (videoFile.length > getVideoMaxUploadSize(serviceInfo)) {
            String msg = messageSource.getMessage("error.upload.size.video",
                                        new Object[]{FileSizeFormatUtil.formatBytes(
                                                        defaultVideoMaxUploadSize, "MB")}, locale);
            return VideoSubliminalGenerateResponse.newBuilder().setMessage(msg).build();
        }
        int imageMaxUploadSize = getImageMaxUploadSize(serviceInfo);
        for (SubliminalImageInsert insert : request.getInsertsList()) {
            if (insert.getImageFile().size() > imageMaxUploadSize) {
                String msg = messageSource.getMessage("error.upload.size.image", 
                                            new Object[]{FileSizeFormatUtil.formatBytes(
                                                        defaultImageMaxUploadSize, "MB")}, locale);
                return VideoSubliminalGenerateResponse.newBuilder().setMessage(msg).build();
            }
        }
        int maxInsertNum = Integer.MAX_VALUE;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
            if (node.has("image_insert_num")) {
                maxInsertNum = node.get("image_insert_num").asInt(Integer.MAX_VALUE);
            }
        } catch (Exception e) {
            log.warn("image_insert_num取得失敗", e);
        }
        System.out.println("maxInsertNum: " + maxInsertNum);
        if (request.getInsertsList().size() > maxInsertNum) {
            String msg = messageSource.getMessage("error.image_insert_num.limit",
                new Object[]{maxInsertNum}, locale);
                System.out.println("msg: " + msg);
            return VideoSubliminalGenerateResponse.newBuilder().setMessage(msg).build();
        }
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        File tempFile = null;
        try {
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
            if (tempDirPath == null || tempDirPath.isEmpty() || !new File(tempDirPath).exists()) {
                String msg = messageSource.getMessage("error.temp_dir_not_set", 
                                            new Object[]{defaultImageMaxUploadSize}, locale);
                return VideoSubliminalGenerateResponse.newBuilder().setMessage(msg).build();
            }
            String uniqueName = UUID.randomUUID().toString() + ".mp4";
            tempFile = new File(tempDirPath, uniqueName);
            grabber = new FFmpegFrameGrabber(new ByteArrayInputStream(videoFile));
            grabber.start();
            double frameRate = grabber.getFrameRate();
            int imageWidth = grabber.getImageWidth();
            int imageHeight = grabber.getImageHeight();
            int audioChannels = grabber.getAudioChannels();
            if (audioChannels <= 0) audioChannels = 1;
            Map<Integer, List<SubliminalImageInsert>> insertMap = new HashMap<>();
            for (SubliminalImageInsert insert : request.getInsertsList()) {
                for (int j = 0; j < insert.getDurationFrames(); j++) {
                    int idx = insert.getFrameIndex() + j;
                    insertMap.computeIfAbsent(idx, k -> new ArrayList<>()).add(insert);
                }
            }
            recorder = new FFmpegFrameRecorder(
                                tempFile.getAbsolutePath(), imageWidth, imageHeight, audioChannels);
            recorder.setFormat("mp4");
            recorder.setFrameRate(frameRate);
            recorder.setVideoCodec(grabber.getVideoCodec());
            recorder.setAudioCodec(grabber.getAudioCodec());
            recorder.start();
            int frameIdx = 0;
            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                Frame frame;
                boolean isSynthesis = false;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
                    if (node.has("synthesis")) {
                        isSynthesis = node.get("synthesis").asBoolean(false);
                    }
                } catch (Exception e) {
                    log.warn("synthesis取得失敗", e);
                }
                while ((frame = grabber.grabImage()) != null) {
                    if (frame.image != null && insertMap.containsKey(frameIdx)) {
                        BufferedImage base = converter.convert(frame);
                        for (SubliminalImageInsert insert : insertMap.get(frameIdx)) {
                            try {
                                Mat mat = opencv_imgcodecs.imdecode(
                                    new Mat(insert.getImageFile().toByteArray()),
                                    opencv_imgcodecs.IMREAD_UNCHANGED);
                                BufferedImage overlayImg = matToBufferedImage(mat);
                                if (base != null && overlayImg != null) {
                                    if (isSynthesis) {
                                        // 合成
                                        Graphics2D g = base.createGraphics();
                                        g.drawImage(overlayImg, insert.getX(),
                                                    insert.getY(), null);
                                        g.dispose();
                                    } else {
                                        // 完全な画像差し替え
                                        BufferedImage replaced = new BufferedImage(
                                                    base.getWidth(), base.getHeight(), base.getType());
                                        Graphics2D g = replaced.createGraphics();
                                        g.drawImage(overlayImg, 0, 0, base.getWidth(),
                                                    base.getHeight(), null);
                                        g.dispose();
                                        base = replaced;
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("画像デコード失敗", e);
                            }
                        }
                        org.bytedeco.javacv.Frame newFrame = converter.convert(base);
                        newFrame.timestamp = frame.timestamp;
                        recorder.record(newFrame);
                    } else if (frame.image == null) {
                        recorder.record(frame);
                    } else {
                        recorder.record(frame);
                    }
                    frameIdx++;
                }
            }
            grabber.stop();
            grabber.close();
            // 音声ストリームを再grab
            FFmpegFrameGrabber audioGrabber = null;
            try {
                audioGrabber = new FFmpegFrameGrabber(new ByteArrayInputStream(videoFile));
                audioGrabber.start();
                Frame audioFrame;
                while ((audioFrame = audioGrabber.grabSamples()) != null) {
                    recorder.record(audioFrame);
                }
                audioGrabber.stop();
                audioGrabber.close();
            } catch (Exception e) {
                log.warn("音声ストリーム抽出失敗", e);
            }
            recorder.stop();
            byte[] processedVideo = Files.readAllBytes(tempFile.toPath());
            return VideoSubliminalGenerateResponse.newBuilder()
                    .setProcessedVideoFile(ByteString.copyFrom(processedVideo))
                    .setMessage(messageSource.getMessage("info.subliminal.generate.success",
                            null, locale))
                    .build();
        } catch (Exception e) {
            log.error("サブミナル動画生成処理でエラー", e);
            String msg = messageSource.getMessage("error.subliminal.generate", null, locale);
            return VideoSubliminalGenerateResponse.newBuilder().setMessage(msg).build();
        } finally {
            if (grabber != null) {
                try { grabber.stop(); grabber.close(); }
                catch (Exception ignore) {
                    log.warn("FFmpegFrameGrabber停止時失敗", ignore);
                }
            }
            if (recorder != null) {
                try { recorder.stop(); recorder.close(); }
                catch (Exception ignore) {
                    log.warn("FFmpegFrameRecorder停止時失敗", ignore);
                }
            }
            if (tempFile != null && tempFile.exists()) {
                try { tempFile.delete(); }
                catch (Exception ignore) {
                    log.warn("一時ファイル削除時に失敗", ignore);
                }
            }
        }
    }

    /**
     * BufferedImageのディープコピー
     * @param bi コピー元のBufferedImage
     * @return コピーされたBufferedImage
     */
    private BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage copy = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return copy;
    }

    /**
     * サブミナル判定のためのピクセル差分計算
     * @param img1 前のフレーム画像
     * @param img2 次のフレーム画像
     * @param threshold ピクセル差分閾値
     * @param frameIdx1 前フレームインデックス
     * @param frameIdx2 次フレームインデックス
     * @return サブミナルと判定された場合true
     */
    private boolean isSubliminalByPixelDiffRegion(BufferedImage img1, BufferedImage img2,
                                            double threshold, int frameIdx1, int frameIdx2) {
        int w = PIXEL_DIFF_REGION_WIDTH, h = PIXEL_DIFF_REGION_HEIGHT;
        int x = (img1.getWidth() - w) / 2;
        int y = (img1.getHeight() - h) / 2;
        BufferedImage sub1 = img1.getSubimage(x, y, w, h);
        BufferedImage sub2 = img2.getSubimage(x, y, w, h);
        long diffSum = 0;
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int rgb1 = sub1.getRGB(dx, dy);
                int rgb2 = sub2.getRGB(dx, dy);
                int dr = ((rgb1 >> 16) & 0xff) - ((rgb2 >> 16) & 0xff);
                int dg = ((rgb1 >> 8) & 0xff) - ((rgb2 >> 8) & 0xff);
                int db = (rgb1 & 0xff) - (rgb2 & 0xff);
                diffSum += Math.abs(dr) + Math.abs(dg) + Math.abs(db);
            }
        }
        double avgDiff = diffSum / (double)(w * h * 3);
        return avgDiff > threshold;
    }

    /**
     * 動画のサブミナルチェックを行う
     * @param request チェックリクエスト
     * @return チェック結果を含むレスポンス
     * @throws Exception チェック処理中のエラー
     */
    public VideoSubliminalCheckResponse checkSubliminal(VideoSubliminalCheckRequest request) {
        String accessKey = request.getAccessKey();
        if (accessKey == null || !accessKeyService.isValid(accessKey)) {
            String msg = messageSource.getMessage("error.accesskey.invalid",
                                                new Object[]{}, locale);
            return VideoSubliminalCheckResponse.newBuilder().setMessage(msg).build();
        }
        String analysisName = request.getAnalysisName();
        ServiceInfo serviceInfo = serviceInfoService.findAll().stream()
            .filter(svcInfo -> analysisName != null && analysisName.equals(svcInfo.getAnalysisName()))
            .findFirst().orElse(null);

        if (serviceInfo == null) {
            String msg = messageSource.getMessage("error.analysis.notfound",
                                        new Object[]{analysisName}, locale);
            return VideoSubliminalCheckResponse.newBuilder().setMessage(msg).build();
        }
        byte[] videoFile = request.getVideoFile().toByteArray();
        if (videoFile.length > defaultVideoMaxUploadSize) {
            String msg = messageSource.getMessage("error.upload.size.video",
                                        new Object[]{FileSizeFormatUtil.formatBytes(
                                                        defaultVideoMaxUploadSize, "MB")}, locale);
            return VideoSubliminalCheckResponse.newBuilder().setMessage(msg).build();
        }
        FFmpegFrameGrabber grabber = null;
        try {
            grabber = new FFmpegFrameGrabber(new ByteArrayInputStream(videoFile));
            grabber.start();
            int totalFrames = grabber.getLengthInFrames();
            List<SubliminalDetected> detectedList = new ArrayList<>();
            BufferedImage imgPrev = null;
            double threshold = getSubliminalThreshold(serviceInfo);
            int maxDetected = getSubliminalMaxDetected(serviceInfo);
            int detectedCount = 0;
            for (int i = 0; i < totalFrames; i++) {
                Frame curr = grabber.grabImage();
                if (curr == null) break;
                try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                    BufferedImage imgCurr = converter.convert(curr);
                    if (i == 0) {
                        imgPrev = deepCopy(imgCurr);
                    } else {
                        boolean isSubliminalPrev = imgPrev != null && imgCurr != null 
                                && isSubliminalByPixelDiffRegion(imgPrev, imgCurr, threshold, i-1, i);
                        if (isSubliminalPrev) {
                            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
                            ImageIO.write(imgCurr, "png", imgOut);
                            detectedList.add(SubliminalDetected.newBuilder()
                                            .setFrameIndex(i)
                                            .setDetectedImage(
                                                ByteString.copyFrom(
                                                    imgOut.toByteArray()))
                                            .build());
                            detectedCount++;
                            if (detectedCount >= maxDetected) break;
                        }
                    }
                    imgPrev = deepCopy(imgCurr);
                }
                if (detectedCount >= maxDetected) break;
            }
            return VideoSubliminalCheckResponse.newBuilder()
                    .addAllDetected(detectedList)
                    .setMessage(messageSource.getMessage("info.subliminal.check.success",
                            null, locale))
                    .build();
        } catch (Exception e) {
            log.error("サブミナルチェック処理でエラー", e);
            String msg = messageSource.getMessage("error.subliminal.check", null, locale);
            return VideoSubliminalCheckResponse.newBuilder().setMessage(msg).build();
        } finally {
            if (grabber != null) {
                try { grabber.stop(); grabber.close(); }
                catch (Exception ignore) {
                    log.warn("FFmpegFrameGrabber停止時のエラー", ignore);
                }
            }
        }
    }

    /**
     * MatをBufferedImageに変換
     * @param mat OpenCVのMatオブジェクト
     * @return BufferedImageオブジェクト
     */
    private BufferedImage matToBufferedImage(Mat mat) {
        try (ToMat matConv = new OpenCVFrameConverter.ToMat();
             Java2DFrameConverter converter = new Java2DFrameConverter()) {
            Frame frame = matConv.convert(mat);
            return converter.convert(frame);
        }
    }

    /**
     * 動画アップロード最大値(JSON→設定ファイル)
     * @param serviceInfo サービス情報
     * @return 動画アップロード最大値
     * @throws Exception JSONパースエラー
     */
    private int getVideoMaxUploadSize(ServiceInfo serviceInfo) {
        int videoMaxUploadSize = defaultVideoMaxUploadSize;
        if (serviceInfo != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
                if (node.has("video_max_upload_size")) {
                    videoMaxUploadSize = node.get("video_max_upload_size")
                                                .asInt(videoMaxUploadSize);
                }
            } catch (Exception e) {
                log.warn("video_max_upload_size取得時のJSONパースエラー", e);
            }
        }
        return videoMaxUploadSize;
    }

    /**
     * 画像アップロード最大値(JSON→設定ファイル)
     * @param serviceInfo サービス情報
     * @return 画像アップロード最大値
     * @throws Exception JSONパースエラー
     */
    private int getImageMaxUploadSize(ServiceInfo serviceInfo) {
        int imageMaxUploadSize = defaultImageMaxUploadSize;
        if (serviceInfo != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
                if (node.has("image_max_upload_size")) {
                    imageMaxUploadSize = node.get("image_max_upload_size")
                                                .asInt(imageMaxUploadSize);
                }
            } catch (Exception e) {
                log.warn("image_max_upload_size取得時のJSONパースエラー", e);
            }
        }
        return imageMaxUploadSize;
    }

    /**
     * サブミナル判定の閾値を取得(JSON→設定ファイル)
     * @param serviceInfo サービス情報
     * @return サブミナル判定の閾値
     * @throws Exception JSONパースエラー
     */
    private double getSubliminalThreshold(ServiceInfo serviceInfo) {
        double threshold = defaultSubliminalThreshold;
        if (serviceInfo != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
                if (node.has("threshold")) {
                    threshold = node.get("threshold").asDouble(threshold);
                }
            } catch (Exception e) {
                log.warn("threshold取得時のJSONパースエラー", e);
            }
        }
        return threshold;
    }

    /**
     * サブミナル検出の最大検出件数(JSON→設定ファイル)
     * @param serviceInfo サービス情報
     * @return 最大検出件数
     * @throws Exception JSONパースエラー
     */
    private int getSubliminalMaxDetected(ServiceInfo serviceInfo) {
        int maxDetected = defaultSubliminalMaxDetected;
        if (serviceInfo != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(serviceInfo.getDataProcessInfoJson());
                if (node.has("max_detected")) {
                    maxDetected = node.get("max_detected").asInt(maxDetected);
                }
            } catch (Exception e) {
                log.warn("max_detected取得時のJSONパースエラー", e);
            }
        }
        return maxDetected;
    }
}
