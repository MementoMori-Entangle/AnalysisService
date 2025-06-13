package com.entangle.analysis.controller;

import com.entangle.analysis.entity.ServiceInfo;
import com.entangle.analysis.entity.Pattern;
import com.entangle.analysis.service.ServiceInfoService;
import com.entangle.analysis.repository.PatternRepository;
import com.entangle.analysis.repository.LogRepository;
import com.entangle.analysis.entity.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.entangle.analysis.util.TemplateImageInfoUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;

@Controller
@RequestMapping("/admin/service-info")
public class ServiceInfoController {
    @Autowired
    private ServiceInfoService serviceInfoService;

    @Autowired
    private PatternRepository patternRepository;

    @Autowired
    private LogRepository logRepository;

    @Value("classpath:analysis_types.yml")
    private Resource analysisTypesResource;

    @Value("${template.images.page-size:10}")
    private int templateImagesPageSize;

    @Lazy
    @Autowired
    private TemplateImageInfoUtil templateImageInfoUtil;

    @Autowired
    private MessageSource messageSource;

    // フィールドはThymeleafで利用されているため警告は無視してOK
    @SuppressWarnings({"WeakerAccess", "unused"})
    private static class AnalysisType {
        public String id;
        public String label;
        public String jsonExample;  
    }

    private ArrayList<AnalysisType> loadAnalysisTypes() {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(analysisTypesResource.getInputStream());
            ArrayList<AnalysisType> result = new ArrayList<>();
            if (data != null && data.containsKey("analysis-types")) {
                for (Object obj : (Iterable<?>)data.get("analysis-types")) {
                    if (obj instanceof LinkedHashMap) {
                        @SuppressWarnings("unchecked")
                        LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) obj;
                        AnalysisType t = new AnalysisType();
                        t.id = (String)map.get("id");
                        t.label = (String)map.get("label");
                        t.jsonExample = (String)map.get("jsonExample");
                        result.add(t);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @GetMapping({"", "/"})
    public String list(Model model, HttpServletRequest request) {
        List<ServiceInfo> list = serviceInfoService.findAll();
        // templateDirPathを抽出してMapで渡す
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> viewList = new ArrayList<>();
        for (ServiceInfo info : list) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", info.getId());
            map.put("analysisType", info.getAnalysisType());
            map.put("analysisName", info.getAnalysisName());
            map.put("dataProcessInfoJson", info.getDataProcessInfoJson());
            String templateDirPath = null;
            try {
                JsonNode node = mapper.readTree(info.getDataProcessInfoJson());
                if (node.has("templateDirPath")) {
                    templateDirPath = node.get("templateDirPath").asText();
                }
            } catch (Exception e) { /* ignore */ }
            map.put("templateDirPath", templateDirPath);
            viewList.add(map);
        }
        model.addAttribute("serviceInfoList", viewList);
        logAction(request, messageSource.getMessage("serviceinfo.list.screen", null, request.getLocale()),
                  messageSource.getMessage("serviceinfo.list.action", null, request.getLocale()));
        return "admin/service_info_list";
    }

    @GetMapping({"/new", "/edit/{id}"})
    public String form(@PathVariable(required = false) Long id, Model model, HttpServletRequest request) {
        ServiceInfo info = (id != null) ? serviceInfoService.findById(id).orElse(new ServiceInfo()) : new ServiceInfo();
        model.addAttribute("serviceInfo", info);
        model.addAttribute("mode", (id != null) ? "edit" : "new");
        model.addAttribute("analysisTypes", loadAnalysisTypes());
        logAction(request, messageSource.getMessage("serviceinfo.form.screen", null, request.getLocale()),
                  id != null ? messageSource.getMessage("serviceinfo.form.edit.action", new Object[]{id}, request.getLocale())
                             : messageSource.getMessage("serviceinfo.form.new.action", null, request.getLocale()));
        return "admin/service_info_form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute @Valid ServiceInfo serviceInfo, BindingResult result, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        if (result.hasErrors()) {
            return "admin/service_info_form";
        }
        serviceInfoService.save(serviceInfo);
        logAction(request, messageSource.getMessage("serviceinfo.entity", null, request.getLocale()),
                  messageSource.getMessage("serviceinfo.create.action", new Object[]{serviceInfo.getId()}, request.getLocale()));
        redirectAttributes.addFlashAttribute("msg", messageSource.getMessage("serviceinfo.create.success", null, request.getLocale()));
        return "redirect:/admin/service-info";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute @Valid ServiceInfo serviceInfo, BindingResult result, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        if (result.hasErrors()) {
            return "admin/service_info_form";
        }
        serviceInfo.setId(id);
        serviceInfoService.save(serviceInfo);
        logAction(request, messageSource.getMessage("serviceinfo.entity", null, request.getLocale()),
                  messageSource.getMessage("serviceinfo.update.action", new Object[]{serviceInfo.getId()}, request.getLocale()));
        redirectAttributes.addFlashAttribute("msg", messageSource.getMessage("serviceinfo.update.success", null, request.getLocale()));
        return "redirect:/admin/service-info";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        serviceInfoService.deleteById(id);
        logAction(request, messageSource.getMessage("serviceinfo.entity", null, request.getLocale()),
                  messageSource.getMessage("serviceinfo.delete.action", new Object[]{id}, request.getLocale()));
        redirectAttributes.addFlashAttribute("msg", messageSource.getMessage("serviceinfo.delete.success", null, request.getLocale()));
        return "redirect:/admin/service-info";
    }

    @GetMapping("/template-images")
    public String listTemplateImages(@RequestParam(required = false) String dir, @RequestParam(name = "page", required = false, defaultValue = "1") int page, Model model, HttpServletRequest request) {
        List<String> imagePaths = new ArrayList<>();
        List<String> encodedImagePaths = new ArrayList<>();
        Map<String, Boolean> imageRegisteredMap = new LinkedHashMap<>();
        int totalCount = 0;
        int totalPages = 1;
        if (dir != null && !dir.isEmpty()) {
            try {
                Path dirPath = Paths.get(dir);
                if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                    List<String> allImagePaths = Files.list(dirPath)
                            .filter(p -> !Files.isDirectory(p))
                            .filter(p -> {
                                String name = p.getFileName().toString().toLowerCase();
                                return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".bmp");
                            })
                            .map(p -> new File(p.toAbsolutePath().toString()).getAbsolutePath().replace('\\', '/').trim())
                            .collect(Collectors.toList());
                    int total = allImagePaths.size();
                    totalCount = total;
                    totalPages = (int)Math.ceil((double)total / templateImagesPageSize);
                    if (page < 1) page = 1;
                    if (page > totalPages) page = totalPages;
                    int fromIdx = (page - 1) * templateImagesPageSize;
                    int toIdx = Math.min(fromIdx + templateImagesPageSize, total);
                    if (fromIdx < toIdx) {
                        imagePaths = allImagePaths.subList(fromIdx, toIdx);
                    }
                    for (String path : imagePaths) {
                        encodedImagePaths.add(URLEncoder.encode(path, StandardCharsets.UTF_8));
                    }
                    List<Pattern> registeredPatterns = patternRepository.findAll();
                    List<String> registeredPaths = registeredPatterns.stream()
                        .map(p -> new File(p.getImagePath()).getAbsolutePath().replace('\\', '/').trim())
                        .collect(Collectors.toList());
                    for (String path : imagePaths) {
                        boolean isRegistered = registeredPaths.stream().anyMatch(r -> r.equals(path));
                        imageRegisteredMap.put(new String(path.trim()), isRegistered ? true : false);
                    }
                }
            } catch (Exception e) {
                // ignore or log
            }
        }
        model.addAttribute("imagePaths", imagePaths);
        model.addAttribute("encodedImagePaths", encodedImagePaths);
        model.addAttribute("imageRegisteredMap", imageRegisteredMap);
        model.addAttribute("templateDirPath", dir);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", templateImagesPageSize);
        model.addAttribute("totalCount", totalCount);
        logAction(request, messageSource.getMessage("templateimage.screen", null, request.getLocale()),
                  messageSource.getMessage("templateimage.list.action", new Object[]{dir, page}, request.getLocale()));
        return "admin/template_image_select";
    }

    @PostMapping("/register-templates")
    public String registerTemplates(@RequestParam(value = "selectedImages", required = false) List<String> selectedImages,
                                    @RequestParam("templateDirPath") String templateDirPath,
                                    RedirectAttributes redirectAttributes, HttpServletRequest request) {
        if (selectedImages == null) selectedImages = new ArrayList<>();
        // getAbsolutePath+区切り文字統一して比較
        List<String> selectedNormalized = selectedImages.stream()
            .map(p -> new File(p).getAbsolutePath().replace('\\', '/'))
            .collect(Collectors.toList());
        List<Pattern> registeredPatterns = patternRepository.findAll();
        List<String> registeredPaths = registeredPatterns.stream()
            .map(p -> new File(p.getImagePath()).getAbsolutePath().replace('\\', '/'))
            .collect(Collectors.toList());
        int addCount = 0;
        int removeCount = 0;
        // 追加: チェックされた画像でDB未登録のものを登録
        for (String path : selectedNormalized) {
            if (!registeredPaths.contains(path)) {
                File file = new File(path);
                if (file.exists()) {
                    Pattern pattern = new Pattern();
                    pattern.setName(file.getName());
                    pattern.setImagePath(file.getAbsolutePath().replace('\\', '/'));
                    patternRepository.save(pattern);
                    addCount++;
                }
            }
        }
        // 削除: DB登録済み画像で今回チェックされなかったものを削除
        for (Pattern pattern : registeredPatterns) {
            String path = new File(pattern.getImagePath()).getAbsolutePath().replace('\\', '/');
            if (path != null && path.startsWith(new File(templateDirPath).getAbsolutePath().replace('\\', '/')) && !selectedNormalized.contains(path)) {
                patternRepository.delete(pattern);
                removeCount++;
            }
        }
        logAction(request, messageSource.getMessage("templateimage.screen", null, request.getLocale()),
                  messageSource.getMessage("templateimage.register.action", new Object[]{addCount, removeCount}, request.getLocale()));
        redirectAttributes.addFlashAttribute("msg", messageSource.getMessage("templateimage.register.success", new Object[]{addCount, removeCount}, request.getLocale()));
        return "redirect:/admin/service-info/template-images?dir=" + java.net.URLEncoder.encode(templateDirPath, java.nio.charset.StandardCharsets.UTF_8);
    }

    @GetMapping("/image-preview")
    @ResponseBody
    public ResponseEntity<byte[]> imagePreview(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return ResponseEntity.notFound().build();
            String mime = Files.probeContentType(file.toPath());
            byte[] bytes = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime)).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 管理画面トップ（index）表示
    @GetMapping("/admin/index")
    public String adminIndex() {
        return "admin/index";
    }

    // ログ記録用メソッド
    private void logAction(HttpServletRequest request, String screen, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
        String path = request.getRequestURI();
        Log log = new Log();
        log.setUsername(username);
        log.setPath(path);
        log.setScreen(screen);
        log.setAction(action);
        log.setIpAddress(request.getRemoteAddr());
        logRepository.save(log);
    }
}
