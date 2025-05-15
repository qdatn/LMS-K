package com.example.hcm25_cpl_ks_java_01_lms.assessmentType;

import com.example.hcm25_cpl_ks_java_01_lms.assessment.Assessment;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
@Entity
@Builder
public class AssessmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Override
    public String toString() {
        return name;
    }

    @OneToMany(mappedBy = "assessmentType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assessment> assessments;
}
