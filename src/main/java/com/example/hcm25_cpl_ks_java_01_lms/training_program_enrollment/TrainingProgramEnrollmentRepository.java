package com.example.hcm25_cpl_ks_java_01_lms.training_program_enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingProgramEnrollmentRepository extends JpaRepository<TrainingProgramEnrollment, Long> {

    // Lấy danh sách enrollment theo chương trình đào tạo
    List<TrainingProgramEnrollment> findByTrainingProgramId(Long trainingProgramId);

    // Lấy danh sách enrollment theo lớp
    List<TrainingProgramEnrollment> findByEnrolledClassId(Long classId);

    // Xoá theo ID (đã có sẵn, ghi rõ lại để dễ gọi)
    void deleteById(Long id);
}

