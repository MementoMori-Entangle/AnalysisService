package com.entangle.analysis.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal() == null ||
            authentication.getPrincipal().equals("anonymousUser") ||
            authentication.getAuthorities() == null ||
            authentication.getAuthorities().isEmpty()) {
            return "redirect:/login";
        }
        return "redirect:/admin";
    }
}
