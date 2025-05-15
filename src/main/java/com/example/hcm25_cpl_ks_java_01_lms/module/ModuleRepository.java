package com.example.hcm25_cpl_ks_java_01_lms.module;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    Page<Module> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByNameOrUrl(String name, String url);

    Module findByNameOrUrl(String name, String url);

    List<Module> findByRoles_Name(String role);

    Module findByName(String name);

    Module findByUrl(String url);

    @Modifying
    @Query("DELETE FROM Module m WHERE m.id = :id")
    void deleteById(Long id);

    @Modifying
    @Query("DELETE FROM Module m WHERE m.id IN :ids")
    void deleteAllById(List<Long> ids);

    @Modifying
    @Query("DELETE FROM Module m")
    void deleteAll();

    List<Module> findByModuleGroupId(Long moduleGroupId);
}
