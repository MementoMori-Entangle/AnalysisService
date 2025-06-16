package com.entangle.analysis.repository;

import com.entangle.analysis.entity.ServiceInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceInfoRepository extends JpaRepository<ServiceInfo, Long> {
    List<ServiceInfo> findByAnalysisTypeContainingAndAnalysisNameContaining(String analysisType, String analysisName);
}
