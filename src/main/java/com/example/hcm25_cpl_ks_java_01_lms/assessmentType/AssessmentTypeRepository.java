package com.example.hcm25_cpl_ks_java_01_lms.assessmentType;

import com.example.hcm25_cpl_ks_java_01_lms.language.Language;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssessmentTypeRepository extends JpaRepository<AssessmentType, Integer> {
    Page<AssessmentType> findByNameContainingIgnoreCase(String searchTerm, Pageable pageable);
    Optional<AssessmentType> findByName(String name);
}


