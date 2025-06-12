package com.entangle.analysis.repository;

import com.entangle.analysis.entity.GrpcAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrpcAccessLogRepository extends JpaRepository<GrpcAccessLog, Long> {
}
