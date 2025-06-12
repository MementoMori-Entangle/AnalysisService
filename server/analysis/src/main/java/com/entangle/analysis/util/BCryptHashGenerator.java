package com.entangle.analysis.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptHashGenerator {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java BCryptHashGenerator <raw_password>");
            System.exit(1);
        }
        String rawPassword = args[0];
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(rawPassword);
        System.out.println(hash);
    }
}
