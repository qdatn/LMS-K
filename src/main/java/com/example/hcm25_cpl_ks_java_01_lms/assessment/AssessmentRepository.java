package com.example.hcm25_cpl_ks_java_01_lms.assessment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Page<Assessment> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Assessment> findByCourseId(Long courseId, Pageable pageable);
    Page<Assessment> findByAssessmentTypeId(Integer assessmentTypeId, Pageable pageable);

    List<Assessment> findAllByCourseId(Long courseId);
    List<Assessment> findAllByAssessmentTypeId(Integer assessmentTypeId);

    Page<Assessment> findByTitleContainingIgnoreCaseAndCourseId(String title, Long courseId, Pageable pageable);
    Page<Assessment> findByTitleContainingIgnoreCaseAndAssessmentTypeId(String title, Integer assessmentTypeId, Pageable pageable);

    Page<Assessment> findAll(Pageable pageable);

    Optional<Assessment> findByTitle(String title);

    @EntityGraph(attributePaths = {"exercises", "questions"})
    Optional<Assessment> findById(Long id);
}
