package com.entangle.analysis.repository;

import com.entangle.analysis.entity.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternRepository extends JpaRepository<Pattern, Long> {
}
