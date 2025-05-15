package com.example.hcm25_cpl_ks_java_01_lms.assessment;

import com.example.hcm25_cpl_ks_java_01_lms.answer.Answer;
import com.example.hcm25_cpl_ks_java_01_lms.assessmentType.AssessmentType;
import com.example.hcm25_cpl_ks_java_01_lms.assessmentType.AssessmentTypeExcelImporter;
import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import com.example.hcm25_cpl_ks_java_01_lms.course.Course;
import com.example.hcm25_cpl_ks_java_01_lms.department.DepartmentController;
import com.example.hcm25_cpl_ks_java_01_lms.question.Question;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.hcm25_cpl_ks_java_01_lms.assessmentType.AssessmentType;
import com.example.hcm25_cpl_ks_java_01_lms.assessmentType.AssessmentTypeService;
import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import com.example.hcm25_cpl_ks_java_01_lms.course.Course;
import com.example.hcm25_cpl_ks_java_01_lms.course.CourseService;
import com.example.hcm25_cpl_ks_java_01_lms.exercise.Exercise;
import com.example.hcm25_cpl_ks_java_01_lms.exercise.ExerciseService;
import com.example.hcm25_cpl_ks_java_01_lms.language.Language;
import com.example.hcm25_cpl_ks_java_01_lms.language.LanguageService;
import com.example.hcm25_cpl_ks_java_01_lms.quiz.Quiz;
import com.example.hcm25_cpl_ks_java_01_lms.quiz.QuizService;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/assessments")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Assessment')")
public class AssessmentController {

    private final AssessmentService assessmentService;

    private final AssessmentTypeService assessmentTypeService;

    private final CourseService courseService;

    private final LanguageService languageService;

    private final ExerciseService exerciseService;
    private final QuizService quizService;

    public AssessmentController(AssessmentService assessmentService, AssessmentTypeService assessmentTypeService, CourseService courseService, LanguageService languageService, ExerciseService exerciseService, QuizService quizService) {
        this.courseService=courseService;
        this.assessmentService = assessmentService;
        this.assessmentTypeService = assessmentTypeService;
        this.languageService=languageService;
        this.exerciseService=exerciseService;
        this.quizService=quizService;
    }

    @GetMapping
    public String getAllAssessments(Model model,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "8") int size,
                                    @RequestParam(required = false) String searchTerm,
                                    @RequestParam(required = false) Integer typeId) {
        Page<Assessment> assessments = assessmentService.searchAssessments(searchTerm, typeId, page, size);
        List<Course> courses = courseService.getAllCourses();
        List<AssessmentType> assessmentTypes=assessmentTypeService.getAllAssessmentTypes();
        model.addAttribute("assessments", assessments);
        model.addAttribute("assessmentTypes", assessmentTypes);
        model.addAttribute("courses", courses);
        model.addAttribute("selectedType", typeId);
        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("content", "assessments/list");
        return Constants.LAYOUT;
    }

    @GetMapping("/{id}")
    public String getAssessmentById(@PathVariable Long id, Model model) {
        Optional<Assessment> assessmentOpt = assessmentService.getAssessmentById(id);

        if (assessmentOpt.isPresent()) {
            Assessment assessment = assessmentOpt.get();

            model.addAttribute("assessment", assessment);
            model.addAttribute("exercises", assessment.getExercises());
            model.addAttribute("questions", assessment.getQuestions());

            model.addAttribute("content", "assessments/detail");
            return Constants.LAYOUT;
        } else {
            model.addAttribute("exercises", List.of());
            model.addAttribute("questions", List.of());
            return "redirect:/assessments";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        List<AssessmentType> assessmentTypes=assessmentTypeService.getAllAssessmentTypes();
        List<Course> courses=courseService.getAllCourses();
        List<Language> languages=languageService.getAllLanguages();
        List<Exercise> exercises=exerciseService.getAllExercises();
        List<Quiz> quizzes=quizService.getAllQuizzes();
        model.addAttribute("assessment", new Assessment());
        model.addAttribute("assessmentTypes", assessmentTypes);
        model.addAttribute("courses", courses);
        model.addAttribute("languages", languages);
        model.addAttribute("exercises", exercises);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("content", "assessments/create");
        return Constants.LAYOUT;
    }


    @PostMapping
    public String createAssessment(@ModelAttribute Assessment assessment, Model model,
                                   @RequestParam("selectedExerciseIds") String selectedExerciseIds,
                                   @RequestParam("selectedQuestionIds") String selectedQuestionIds) {
        try {
            // In dữ liệu ra console để kiểm tra
            System.out.println("Received Assessment Data:");
            System.out.println("Title: " + assessment.getTitle());
            System.out.println("Total Score: " + assessment.getTotalScore());
            System.out.println("Minimum Score: " + assessment.getMinimumScore());
            System.out.println("Time Limit: " + assessment.getTimeLimit());
            System.out.println("Assessment Type ID: " + (assessment.getAssessmentType() != null ? assessment.getAssessmentType().getId() : "NULL"));
            System.out.println("Course ID: " + (assessment.getCourse() != null ? assessment.getCourse().getId() : "NULL"));
            System.out.println("Exercise IDs: " + selectedExerciseIds);
            System.out.println("Question IDs: " + selectedQuestionIds);

            // Chuyển đổi selectedExerciseIds và selectedQuestionIds thành danh sách
            List<Long> exerciseIds = new ArrayList<>();
            if (selectedExerciseIds != null && !selectedExerciseIds.trim().isEmpty()) {
                exerciseIds = Arrays.stream(selectedExerciseIds.split(","))
                        .filter(id -> !id.trim().isEmpty()) // Loại bỏ các phần tử rỗng
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            }

            List<Long> questionIds = new ArrayList<>();
            if (selectedQuestionIds != null && !selectedQuestionIds.trim().isEmpty()) {
                questionIds = Arrays.stream(selectedQuestionIds.split(","))
                        .filter(id -> !id.trim().isEmpty()) // Loại bỏ các phần tử rỗng
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            }

            // Lưu assessment và các bài tập, câu hỏi liên quan
            assessmentService.saveAssessment(assessment, exerciseIds, questionIds);

            return "redirect:/assessments";
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Invalid ID format: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred: " + e.getMessage());
        }

        // Nếu có lỗi, quay lại trang tạo assessment với thông báo lỗi
        model.addAttribute("assessment", assessment);
        model.addAttribute("content", "assessments/create");
        return Constants.LAYOUT;
    }


    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Assessment assessment = assessmentService.getAssessmentById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found with ID: " + id));

        // Lấy danh sách ID của exercises và questions từ danh sách liên kết @ManyToMany
        List<Long> exerciseIds = assessment.getExercises().stream()
                .map(Exercise::getId)
                .collect(Collectors.toList());

        List<Long> questionIds = assessment.getQuestions().stream()
                .map(Question::getId)
                .collect(Collectors.toList());

        // Truyền dữ liệu vào model thay vì gán vào assessment
        model.addAttribute("assessment", assessment);
        model.addAttribute("exerciseIds", exerciseIds);
        model.addAttribute("questionIds", questionIds);

        // Lấy dữ liệu cần thiết cho form
        model.addAttribute("assessmentTypes", assessmentTypeService.getAllAssessmentTypes());
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("languages", languageService.getAllLanguages());
        model.addAttribute("exercises", exerciseService.getAllExercises());
        model.addAttribute("quizzes", quizService.getAllQuizzes());
        model.addAttribute("content", "assessments/edit");

        return Constants.LAYOUT;
    }

    @PostMapping("/edit/{id}")
    public String updateAssessment(@PathVariable Long id, @ModelAttribute Assessment assessment, Model model,
                                   @RequestParam("selectedExerciseIds") String selectedExerciseIds,
                                   @RequestParam("selectedQuestionIds") String selectedQuestionIds) {
        try {
            // In dữ liệu ra console để kiểm tra
            System.out.println("Received Assessment Data for Update:");
            System.out.println("Title: " + assessment.getTitle());
            System.out.println("Total Score: " + assessment.getTotalScore());
            System.out.println("Minimum Score: " + assessment.getMinimumScore());
            System.out.println("Time Limit: " + assessment.getTimeLimit());
            System.out.println("Assessment Type ID: " + (assessment.getAssessmentType() != null ? assessment.getAssessmentType().getId() : "NULL"));
            System.out.println("Course ID: " + (assessment.getCourse() != null ? assessment.getCourse().getId() : "NULL"));
            System.out.println("Exercise IDs: " + selectedExerciseIds);
            System.out.println("Question IDs: " + selectedQuestionIds);

            // Chuyển đổi danh sách ID
            List<Long> exerciseIds = parseIdList(selectedExerciseIds);
            List<Long> questionIds = parseIdList(selectedQuestionIds);

            // Cập nhật assessment và các bài tập, câu hỏi liên quan
            assessmentService.updateAssessment(id, assessment, exerciseIds, questionIds);

            return "redirect:/assessments";
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Invalid ID format: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred: " + e.getMessage());
        }

        // Nếu có lỗi, quay lại trang chỉnh sửa assessment với thông báo lỗi
        model.addAttribute("assessment", assessment);
        model.addAttribute("assessmentTypes", assessmentTypeService.getAllAssessmentTypes());
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("languages", languageService.getAllLanguages());
        model.addAttribute("exercises", exerciseService.getAllExercises());
        model.addAttribute("quizzes", quizService.getAllQuizzes());
        model.addAttribute("content", "assessments/edit");

        return Constants.LAYOUT;
    }

    // Hàm parse danh sách ID từ chuỗi
    private List<Long> parseIdList(String ids) {
        if (ids == null || ids.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(ids.split(","))
                .filter(id -> !id.trim().isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    @PostMapping("/delete/{id}")
    public String deleteAssessment(@PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return "redirect:/assessments";
    }

    @GetMapping("/print")
    public String printAssessments(Model model) {
        model.addAttribute("assessments", assessmentService.getAllAssessments());
        return "assessments/print";
    }

    @GetMapping("/preview/{id}")
    public String getPreviewAssessmentById(@PathVariable Long id, Model model) {
        Optional<Assessment> assessmentOpt = assessmentService.getAssessmentById(id);

        if (assessmentOpt.isPresent()) {
            Assessment assessment = assessmentOpt.get();

            model.addAttribute("assessment", assessment);
            model.addAttribute("exercises", assessment.getExercises());
            model.addAttribute("questions", assessment.getQuestions());
            Map<Long, List<Answer>> answersMap = new HashMap<>();
            for (Question question : assessment.getQuestions()) {
                answersMap.put(question.getId(), question.getAnswers());
            }

            model.addAttribute("answersMap", answersMap);

            model.addAttribute("content", "assessments/preview");
            return Constants.LAYOUT;
        } else {
            model.addAttribute("exercises", List.of());
            model.addAttribute("questions", List.of());
            return "redirect:/assessments";
        }
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportToExcel(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        try {
            // Nếu size không được truyền vào, mặc định lấy toàn bộ dữ liệu
            if (size == null) {
                size = (int) assessmentService.countAllAssessments();
            }
            List<Assessment> assessments = assessmentService.getAllAssessments(page, size).getContent();
            ByteArrayInputStream in = assessmentService.exportToExcel(assessments);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=assessments.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(new InputStreamResource(in));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download-template")
    public ResponseEntity<Resource> downloadExcelTemplate() {
        try {
            // Đường dẫn tương đối từ thư mục gốc của project
            Path filePath = Paths.get("data-excel/assessment_template.xlsx");
            Resource resource = new ByteArrayResource(Files.readAllBytes(filePath));

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=assessment_template.xlsx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/delete-all")
    @Transactional
    public ResponseEntity<String> deleteSelectedAssessments(@RequestBody AssessmentController.DeleteRequest deleteRequest, Model model) {
        try {
            List<Long> ids = deleteRequest.getIds();
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("No assessments selected for deletion");
            }
            for (Long id : ids) {
                assessmentService.deleteAssessment(id);
            }
            return ResponseEntity.ok("Assessments deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete assessments: " + e.getMessage());
        }
    }

    // Class để nhận dữ liệu từ request body
    public static class DeleteRequest {
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }

    @PostMapping("/import")
    @Transactional
    public String importExcel(@RequestParam("file") MultipartFile file, Model model) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Please select a file to upload");
            }
            if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
                throw new IllegalArgumentException("Only Excel files (.xlsx, .xls) are supported");
            }
            List<Assessment> assessments = AssessmentExcelImporter.importAssessments(file.getInputStream());
            assessmentService.saveAllFromExcel(assessments);
            return "redirect:/assessments";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to import: " + e.getMessage());
            return Constants.LAYOUT;
        }
    }
}
