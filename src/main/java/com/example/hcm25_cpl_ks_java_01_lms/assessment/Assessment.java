package com.example.hcm25_cpl_ks_java_01_lms.assessment;

import com.example.hcm25_cpl_ks_java_01_lms.assessmentType.AssessmentType;
import com.example.hcm25_cpl_ks_java_01_lms.course.Course;
import com.example.hcm25_cpl_ks_java_01_lms.exercise.Exercise;
import com.example.hcm25_cpl_ks_java_01_lms.question.Question;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "assessment")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Integer totalScore;
    private Integer minimumScore;
    private Integer timeLimit; // Đơn vị: phút

    @ManyToOne
    @JoinColumn(name = "assessment_type_id", nullable = false)
    private AssessmentType assessmentType;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course; // Liên kết với Khóa học

    @ManyToMany
    @JoinTable(
            name = "assessment_exercise",
            joinColumns = @JoinColumn(name = "assessment_id"),
            inverseJoinColumns = @JoinColumn(name = "exercise_id")
    )
    private List<Exercise> exercises= new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "assessment_question",
            joinColumns = @JoinColumn(name = "assessment_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private List<Question> questions= new ArrayList<>();
}