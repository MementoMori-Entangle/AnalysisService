package com.entangle.analysis.controller;

import com.entangle.analysis.entity.AccessKey;
import com.entangle.analysis.repository.AccessKeyRepository;
import com.entangle.analysis.util.AccessKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/access-keys")
public class AccessKeyController {
    @Autowired
    private AccessKeyRepository accessKeyRepository;

    @Value("${access-key.length:32}")
    private int accessKeyLength;

    @GetMapping
    public String list(Model model) {
        List<AccessKey> keys = accessKeyRepository.findAll();
        model.addAttribute("accessKeys", keys);
        return "admin/access_key_list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("accessKey", new AccessKey());
        return "admin/access_key_form";
    }

    @PostMapping
    public String create(@RequestParam("accessKey") String accessKey,
                         @RequestParam(value = "enabled", defaultValue = "false") boolean enabled, Model model) {

        AccessKey entity = accessKeyRepository.findByAccessKey(accessKey);

        if (entity == null) {
            entity = new AccessKey();
            entity.setAccessKey(accessKey);
            entity.setEnabled(enabled);
        } else {
            // 既存のキーがある場合は更新
            entity.setAccessKey(accessKey);
            entity.setEnabled(enabled);
        }

        accessKeyRepository.save(entity);
        return "redirect:/admin/access-keys";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        AccessKey accessKey = accessKeyRepository.findById(id).orElseThrow();
        model.addAttribute("accessKey", accessKey);
        return "admin/access_key_form";
    }

    @PostMapping("/update/{id}")
    public String update(@RequestParam("id") Long id,
                         @RequestParam("accessKey") String accessKey,
                         @RequestParam(value = "enabled", defaultValue = "false") boolean enabled, Model model) {

        AccessKey entity = accessKeyRepository.findById(id).orElseThrow();

        if (entity != null) {
            entity.setAccessKey(accessKey);
            entity.setEnabled(enabled);
        } else {
            model.addAttribute("error", "Access key not found");
            return "admin/access_key_form"; 
        }

        accessKeyRepository.save(entity);
        return "redirect:/admin/access-keys";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        accessKeyRepository.deleteById(id);
        return "redirect:/admin/access-keys";
    }

    @GetMapping("/generate")
    @ResponseBody
    public String generateKey() {
        return AccessKeyGenerator.generate(accessKeyLength);
    }
}
