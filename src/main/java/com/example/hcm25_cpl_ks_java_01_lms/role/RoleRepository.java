package com.example.hcm25_cpl_ks_java_01_lms.role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    
    
    Page<Role> findAll(Pageable pageable);
    Page<Role> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByName(String role);


    @Query("SELECT r FROM Role r WHERE r.name IN (:list)")
    List<Role> findByListName(@Param("list") List<String> list);

    Optional<Role> findByName(String name);

    @Query("SELECT r FROM Role r JOIN r.modules m WHERE m.name = :moduleName")
    List<Role> findByModuleNameContainingIgnoreCase(@Param("moduleName") String moduleName);

    @Query("SELECT r FROM Role r WHERE r.id IN (:list)")
    List<Role> findByListId(List<Long> list);
}
