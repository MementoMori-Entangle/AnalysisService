package com.entangle.analysis.service;

import com.entangle.analysis.entity.AccessKey;
import com.entangle.analysis.repository.AccessKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessKeyService {
    @Autowired
    private AccessKeyRepository accessKeyRepository;

    public boolean isValid(String accessKey) {
        if (accessKey == null || accessKey.isEmpty()) return false;
        AccessKey key = accessKeyRepository.findByAccessKey(accessKey);
        return key != null && key.isEnabled();
    }
}
