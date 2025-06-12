package com.entangle.analysis.service;

import com.entangle.analysis.entity.User;
import com.entangle.analysis.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAuthService {
    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public boolean authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username);
        if (user == null) return false;
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
