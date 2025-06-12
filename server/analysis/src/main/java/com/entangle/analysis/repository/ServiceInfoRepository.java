package com.entangle.analysis.repository;

import com.entangle.analysis.entity.ServiceInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceInfoRepository extends JpaRepository<ServiceInfo, Long> {
}
