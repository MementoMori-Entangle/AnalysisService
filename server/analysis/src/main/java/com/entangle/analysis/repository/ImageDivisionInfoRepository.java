package com.entangle.analysis.repository;

import com.entangle.analysis.entity.ImageDivisionInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageDivisionInfoRepository extends JpaRepository<ImageDivisionInfo, Long> {
    ImageDivisionInfo findByUid(String uid);
}
