package com.entangle.analysis.repository;

import com.entangle.analysis.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Long> {
}
