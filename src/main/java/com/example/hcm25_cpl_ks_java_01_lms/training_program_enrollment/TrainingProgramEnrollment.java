package com.example.hcm25_cpl_ks_java_01_lms.training_program_enrollment;
import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.training_program.TrainingProgram;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
public class TrainingProgramEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "class_id", referencedColumnName = "id")
    private Classes enrolledClass;

    @ManyToOne
    @JoinColumn(name = "training_program_id", referencedColumnName = "id")
    private TrainingProgram trainingProgram;

    @Column(nullable = false)
    private LocalDateTime enrollmentDate;

    @Override
    public String toString() {
        return "TrainingProgramEnrollment{" +
                ", trainingProgram=" + trainingProgram.getProgramName() +
                ", enrollmentDate=" + enrollmentDate +
                '}';
    }
}