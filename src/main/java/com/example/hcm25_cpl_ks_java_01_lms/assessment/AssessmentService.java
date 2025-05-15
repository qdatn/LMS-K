package com.example.hcm25_cpl_ks_java_01_lms.assessment;

import com.example.hcm25_cpl_ks_java_01_lms.assessmentType.AssessmentType;
import com.example.hcm25_cpl_ks_java_01_lms.assessmentType.AssessmentTypeRepository;
import com.example.hcm25_cpl_ks_java_01_lms.assessmentType.AssessmentTypeService;
import com.example.hcm25_cpl_ks_java_01_lms.course.Course;
import com.example.hcm25_cpl_ks_java_01_lms.course.CourseRepository;
import com.example.hcm25_cpl_ks_java_01_lms.course.CourseService;
import com.example.hcm25_cpl_ks_java_01_lms.exercise.Exercise;
import com.example.hcm25_cpl_ks_java_01_lms.exercise.ExerciseRepository;
import com.example.hcm25_cpl_ks_java_01_lms.exercise.ExerciseService;
import com.example.hcm25_cpl_ks_java_01_lms.feedback.assessment.AssessmentFeedback;
import com.example.hcm25_cpl_ks_java_01_lms.feedback.assessment.AssessmentFeedbackDTO;
import com.example.hcm25_cpl_ks_java_01_lms.question.Question;
import com.example.hcm25_cpl_ks_java_01_lms.question.QuestionService;
import com.example.hcm25_cpl_ks_java_01_lms.question.QuestionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final ExerciseService exerciseService;
    private final QuestionService questionService;
    private final AssessmentTypeService assessmentTypeService;
    private final ExerciseRepository exerciseRepository;
    private final QuestionRepository questionRepository;
    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final AssessmentTypeRepository assessmentTypeRepository;

    public List<Assessment> getAllAssessments() {
        return assessmentRepository.findAll();
    }

    public List<AssessmentDTO> getAssessmentsByCourseId(Long courseId) {

        return assessmentRepository.findAllByCourseId(courseId).stream()
                .map(assessment -> convertToDTO(assessment))
                .collect(Collectors.toList());
    }
    private AssessmentDTO convertToDTO(Assessment assessment) {
        AssessmentDTO dto = new AssessmentDTO();
        dto.setId(assessment.getId());
        dto.setTitle(assessment.getTitle());
        return dto;
    }


    public Page<Assessment> getAllAssessments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return assessmentRepository.findAll(pageable);
    }

    public Optional<Assessment> getAssessmentById(Long id) {
        return assessmentRepository.findById(id);
    }

    public long countAllAssessments() {
        return assessmentRepository.count(); // Đếm tổng số dữ liệu
    }

    public Page<Assessment> searchAssessments(String title, Integer typeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (title != null && !title.trim().isEmpty() && typeId != null) {
            return assessmentRepository.findByTitleContainingIgnoreCaseAndAssessmentTypeId(title.trim(), typeId, pageable);
        } else if (title != null && !title.trim().isEmpty()) {
            return assessmentRepository.findByTitleContainingIgnoreCase(title.trim(), pageable);
        } else if (typeId != null) {
            return assessmentRepository.findByAssessmentTypeId(typeId, pageable);
        }
        return assessmentRepository.findAll(pageable);
    }

    @Transactional
    public Assessment saveAssessment(Assessment assessment, List<Long> exerciseIds, List<Long> questionIds) {
        // Thiết lập các thuộc tính cơ bản của Assessment
        Assessment newAssessment = new Assessment();
        newAssessment.setTitle(assessment.getTitle());
        newAssessment.setTotalScore(assessment.getTotalScore());
        newAssessment.setMinimumScore(assessment.getMinimumScore());
        newAssessment.setTimeLimit(assessment.getTimeLimit());

        // Gán AssessmentType
        if (assessment.getAssessmentType() != null) {
            AssessmentType assessmentType = assessmentTypeService.getAssessmentTypeById(assessment.getAssessmentType().getId());
            newAssessment.setAssessmentType(assessmentType);
        }

        // Gán Course
        if (assessment.getCourse() != null) {
            Course course = courseService.getCourseById(assessment.getCourse().getId());
            newAssessment.setCourse(course);
        }
        // Lấy danh sách Exercise theo ID
        if (exerciseIds != null && !exerciseIds.isEmpty()) {
            List<Exercise> exercises = exerciseRepository.findAllById(exerciseIds);
            newAssessment.setExercises(exercises);
        }

        // Lấy danh sách Question theo ID
        if (questionIds != null && !questionIds.isEmpty()) {
            List<Question> questions = questionRepository.findAllById(questionIds);
            newAssessment.setQuestions(questions);
        }

        // Lưu Assessment vào database
        return assessmentRepository.save(newAssessment);
    }

    @Transactional
    public void updateAssessment(Long id, Assessment assessment, List<Long> exerciseIds, List<Long> questionIds) {
        // Tìm Assessment hiện tại trong database
        Assessment existingAssessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found with ID: " + id));

        // Cập nhật thông tin cơ bản
        existingAssessment.setTitle(assessment.getTitle());
        existingAssessment.setTotalScore(assessment.getTotalScore());
        existingAssessment.setMinimumScore(assessment.getMinimumScore());
        existingAssessment.setTimeLimit(assessment.getTimeLimit());

        // Cập nhật AssessmentType
        if (assessment.getAssessmentType() != null) {
            AssessmentType assessmentType = assessmentTypeService.getAssessmentTypeById(assessment.getAssessmentType().getId());
            existingAssessment.setAssessmentType(assessmentType);
        }

        // Cập nhật Course
        if (assessment.getCourse() != null) {
            Course course = courseService.getCourseById(assessment.getCourse().getId());
            existingAssessment.setCourse(course);
        }

        // Cập nhật danh sách Exercise
        if (exerciseIds != null) {
            List<Exercise> exercises = exerciseRepository.findAllById(exerciseIds);
            existingAssessment.setExercises(exercises);
        } else {
            existingAssessment.setExercises(Collections.emptyList()); // Xóa nếu không có bài tập mới
        }

        // Cập nhật danh sách Question
        if (questionIds != null) {
            List<Question> questions = questionRepository.findAllById(questionIds);
            existingAssessment.setQuestions(questions);
        } else {
            existingAssessment.setQuestions(Collections.emptyList()); // Xóa nếu không có câu hỏi mới
        }

        // Lưu vào database
        assessmentRepository.save(existingAssessment);
    }


    public void deleteAssessment(Long id) {
        assessmentRepository.deleteById(id);
    }

    public ByteArrayInputStream exportToExcel(List<Assessment> assessments) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Assessments");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerCellStyle.setFont(font);

            String[] headers = {"ID", "Title", "Total Score", "Minimum Score", "Time Limit", "Assessment Type", "Course", "Exercises", "Questions"};
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Create data rows
            int rowIdx = 1;
            for (Assessment assessment : assessments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(assessment.getId());
                row.createCell(1).setCellValue(assessment.getTitle());
                row.createCell(2).setCellValue(assessment.getTotalScore());
                row.createCell(3).setCellValue(assessment.getMinimumScore());
                row.createCell(4).setCellValue(assessment.getTimeLimit());
                row.createCell(5).setCellValue(assessment.getAssessmentType().getName());
                row.createCell(6).setCellValue(assessment.getCourse().getName());

                String exercises = assessment.getExercises().stream()
                        .map(Exercise::getTitle)
                        .collect(Collectors.joining(", "));
                row.createCell(7).setCellValue(exercises);

                String questions = assessment.getQuestions().stream()
                        .map(Question::getText)
                        .collect(Collectors.joining(", "));
                row.createCell(8).setCellValue(questions);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Transactional
    public List<Assessment> saveAllFromExcel(List<Assessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            throw new IllegalArgumentException("Assessments list cannot be null or empty");
        }

        for (Assessment assessment : assessments) {
            validateAssessment(assessment);
            Optional<Assessment> existing = assessmentRepository.findByTitle(assessment.getTitle());
            if (existing.isPresent()) {
                assessment.setId(existing.get().getId());
            }
            if (assessment.getCourse() != null) {
                Course savedCourse = saveOrGetCourse(assessment.getCourse());
                assessment.setCourse(savedCourse);
            }
            // AssessmentType có thể được xử lý tương tự nếu cần
            if (assessment.getAssessmentType() != null) {
                AssessmentType savedAssessmentType = saveOrGetAssessmentType(assessment.getAssessmentType());
                assessment.setAssessmentType(savedAssessmentType);
            }
        }
        return assessmentRepository.saveAll(assessments);
    }

    private Course saveOrGetCourse(Course course) {
        if (course == null || course.getName() == null || course.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }
        Optional<Course> existingCourse = courseRepository.findByName(course.getName());
        if (existingCourse.isPresent()) {
            return existingCourse.get();
        }
        // Nếu không có thông tin bổ sung, đặt giá trị mặc định nếu cần
        return courseRepository.save(course);
    }

    private AssessmentType saveOrGetAssessmentType(AssessmentType assessmentType) {
        if (assessmentType == null || assessmentType.getName() == null || assessmentType.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("AssessmentType name cannot be null or empty");
        }
        Optional<AssessmentType> existingType = assessmentTypeRepository.findByName(assessmentType.getName());
        if (existingType.isPresent()) {
            return existingType.get();
        }
        return assessmentTypeRepository.save(assessmentType);
    }

    private void validateAssessment(Assessment assessment) {
        if (assessment == null) {
            throw new IllegalArgumentException("Assessment cannot be null");
        }
        if (assessment.getTitle() == null || assessment.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Assessment title cannot be empty");
        }
        if (assessment.getTitle().length() > 255) {
            throw new IllegalArgumentException("Assessment title exceeds maximum length of 255 characters");
        }
        // totalScore, minimumScore, timeLimit có thể null nên không cần validate
        // exercise và question rỗng cũng được phép nên không kiểm tra
    }
}
