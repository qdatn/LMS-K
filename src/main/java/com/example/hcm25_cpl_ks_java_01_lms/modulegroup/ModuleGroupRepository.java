package com.example.hcm25_cpl_ks_java_01_lms.modulegroup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleGroupRepository extends JpaRepository<ModuleGroup, Long> {
    Page<ModuleGroup> findByNameContainingIgnoreCase(String searchTerm, Pageable pageable);

    boolean existsByName(String name);

    ModuleGroup getByName(String name);

    ModuleGroup findByName(String name);
}
