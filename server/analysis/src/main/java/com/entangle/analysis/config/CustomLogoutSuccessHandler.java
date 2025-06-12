package com.entangle.analysis.config;

import com.entangle.analysis.entity.Log;
import com.entangle.analysis.repository.LogRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {
    @Autowired
    private LogRepository logRepository;

    @Autowired
    private MessageSource messageSource;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication != null) {
            Log log = new Log();
            log.setUsername(authentication.getName());
            log.setPath(request.getRequestURI());
            log.setScreen(messageSource.getMessage("login.screen", null, request.getLocale()));
            log.setAction(messageSource.getMessage("logout.success", null, request.getLocale()));
            log.setIpAddress(request.getRemoteAddr());
            logRepository.save(log);
        }
        response.sendRedirect("/login?logout");
    }
}
