package com.entangle.analysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AdminController {
    @Autowired
    private MessageSource messageSource;

    @GetMapping("/admin")
    public String adminHome(Model model, HttpServletRequest request) {
        String title = messageSource.getMessage("admin.index.title", null, request.getLocale());
        model.addAttribute("title", title);
        return "admin/index";
    }
}
