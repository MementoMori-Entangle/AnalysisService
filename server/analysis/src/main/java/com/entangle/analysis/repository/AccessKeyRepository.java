package com.entangle.analysis.repository;

import com.entangle.analysis.entity.AccessKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessKeyRepository extends JpaRepository<AccessKey, Long> {
    AccessKey findByAccessKey(String accessKey);
}
