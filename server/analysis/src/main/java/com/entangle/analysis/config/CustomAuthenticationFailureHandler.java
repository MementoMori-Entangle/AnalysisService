package com.entangle.analysis.config;

import com.entangle.analysis.entity.Log;
import com.entangle.analysis.repository.LogRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Autowired
    private LogRepository logRepository;

    @Autowired
    private MessageSource messageSource;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        Log log = new Log();
        log.setUsername(username != null ? username : "anonymous");
        log.setPath(request.getRequestURI());
        log.setScreen(messageSource.getMessage("login.screen", null, request.getLocale()));
        log.setAction(messageSource.getMessage("login.failed", null, request.getLocale()));
        log.setIpAddress(request.getRemoteAddr());
        logRepository.save(log);
        response.sendRedirect("/login?error");
    }
}
