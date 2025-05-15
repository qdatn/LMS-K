package com.example.hcm25_cpl_ks_java_01_lms.training_program;

import com.example.hcm25_cpl_ks_java_01_lms.classes.ClassesService;
import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import com.example.hcm25_cpl_ks_java_01_lms.course.course_syllabus.CourseSyllabus;
import com.example.hcm25_cpl_ks_java_01_lms.course_enrollment.CourseEnrollmentService;
import com.example.hcm25_cpl_ks_java_01_lms.syllabus.SimpleSyllabusDTO;
import com.example.hcm25_cpl_ks_java_01_lms.syllabus.Syllabus;
import com.example.hcm25_cpl_ks_java_01_lms.syllabus.SyllabusService;
import com.example.hcm25_cpl_ks_java_01_lms.training_program_enrollment.TrainingProgramEnrollmentService;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/training-programs")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'TrainingProgram')")
public class TrainingProgramController {

    private final TrainingProgramService trainingProgramService;
    private final SyllabusService syllabusService;
    private final ClassesService classesService;
    private final TrainingProgramEnrollmentService trainingProgramEnrollmentService;

    private final CourseEnrollmentService courseEnrollmentService;

    private final ClassesService classService;

    private final ObjectMapper objectMapper;

    public TrainingProgramController(TrainingProgramService trainingProgramService,
                                     SyllabusService syllabusService,
                                     ClassesService classesService,
                                     TrainingProgramEnrollmentService trainingProgramEnrollmentService,
                                     CourseEnrollmentService courseEnrollmentService,
                                     ClassesService classService,
                                     ObjectMapper objectMapper) {
        this.trainingProgramService = trainingProgramService;
        this.syllabusService = syllabusService;
        this.classesService = classesService;
        this.trainingProgramEnrollmentService = trainingProgramEnrollmentService;
        this.courseEnrollmentService = courseEnrollmentService;
        this.classService = classService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String listTrainingPrograms(Model model,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String searchTerm) {
        Page<TrainingProgram> trainingPrograms = trainingProgramService.getAllTrainingPrograms(searchTerm, page, size);
        model.addAttribute("trainingPrograms", trainingPrograms);
        model.addAttribute("content", "trainingprograms/list");
        return Constants.LAYOUT;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("trainingProgram", new TrainingProgram());
        model.addAttribute("syllabuses", syllabusService.getAllSyllabus());
        model.addAttribute("content", "trainingprograms/create");
        return Constants.LAYOUT;
    }

    @PostMapping
    public String createTrainingProgram(@ModelAttribute TrainingProgram trainingProgram, Model model) {
        try {
            trainingProgramService.createTrainingProgram(trainingProgram);
            return "redirect:/training-programs";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("trainingProgram", trainingProgram);
            model.addAttribute("syllabuses", syllabusService.getAllSyllabus());
            model.addAttribute("content", "trainingprograms/create");
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        TrainingProgram trainingProgram = trainingProgramService.getTrainingProgramById(id);
        if (trainingProgram != null) {
            model.addAttribute("trainingProgram", trainingProgram);
            model.addAttribute("allSyllabuses", syllabusService.getAllSyllabus());
            model.addAttribute("content", "trainingprograms/update");
            return Constants.LAYOUT;
        }
        return "redirect:/training-programs";
    }

    @PostMapping("/edit/{id}")
    public String updateTrainingProgram(@PathVariable Long id,
                                        @ModelAttribute TrainingProgram trainingProgramDetails,
                                        Model model) {
        try {
            trainingProgramDetails.setId(id);
            trainingProgramService.updateTrainingProgram(trainingProgramDetails);
            return "redirect:/training-programs";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("trainingProgram", trainingProgramDetails);
            model.addAttribute("allSyllabuses", syllabusService.getAllSyllabus());
            model.addAttribute("content", "trainingprograms/update");
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/detail/{id}")
    public String showDetailPage(@PathVariable Long id, Model model) {
        TrainingProgram trainingProgram = trainingProgramService.getTrainingProgramById(id);
        if (trainingProgram != null) {
            model.addAttribute("trainingProgram", trainingProgram);

            // Chuyển syllabuses về SimpleSyllabusDTO
            List<SimpleSyllabusDTO> simpleSyllabuses = trainingProgram.getSyllabuses().stream()
                    .map(s -> SimpleSyllabusDTO.builder()
                            .id(s.getId())
                            .name(s.getName())
                            .syllabusCode(s.getSyllabusCode())
                            .build())
                    .toList();

            model.addAttribute("simpleSyllabuses", simpleSyllabuses); // Thay vì syllabuses gốc
            model.addAttribute("content", "trainingprograms/detail");
            return Constants.LAYOUT;
        }
        return "redirect:/training-programs";
    }


    @GetMapping("/enrollment/{id}")
    public String showEnrollmentPage(@PathVariable Long id,
                                     @RequestParam(required = false) String keyword,
                                     Model model) {
        TrainingProgram trainingProgram = trainingProgramService.getTrainingProgramById(id);
        if (trainingProgram != null) {
            model.addAttribute("trainingProgram", trainingProgram);
            model.addAttribute("classes", trainingProgramEnrollmentService.getAvailableClassesForEnrollment(keyword)); // ✅ truyền keyword
            model.addAttribute("enrolledClasses", trainingProgramEnrollmentService.getEnrollmentsByTrainingProgram(id));
            model.addAttribute("content", "trainingprograms/enrollment");
            return Constants.LAYOUT;
        }
        return "redirect:/training-programs";
    }


    @PostMapping("/enrollment/{id}")
    public String enrollClasses(@PathVariable Long id,
                                @RequestParam List<Long> classIds,
                                RedirectAttributes redirectAttributes) {
        if (classIds != null && !classIds.isEmpty()) {
            trainingProgramEnrollmentService.enrollClasses(id, classIds);
            redirectAttributes.addFlashAttribute("message", "Classes enrolled successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "No class selected.");
        }
        return "redirect:/training-programs/enrollment/" + id;
    }


    @PostMapping("/delete/{id}")
    public String deleteTrainingProgram(@PathVariable Long id) {
        trainingProgramService.deleteTrainingProgram(id);
        return "redirect:/training-programs";
    }

    @PostMapping("/delete-selected")
    public ResponseEntity<?> deleteSelected(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("No IDs provided");
        }

        trainingProgramService.deleteTrainingProgramsByIds(ids);
        return ResponseEntity.ok("Deleted successfully");
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportToExcel(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        try {
            List<TrainingProgram> trainingPrograms = trainingProgramService.getAllTrainingPrograms("", page, size).getContent();
            ByteArrayInputStream in = trainingProgramService.exportToExcel(trainingPrograms);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=training_programs.xlsx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(in));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file để tải lên");
            return "redirect:/training-programs";
        }

        try {
            List<TrainingProgram> trainingPrograms = TrainingProgramExcelImporter.importTrainingPrograms(file.getInputStream());
            trainingProgramService.saveAllFromExcel(trainingPrograms);
            redirectAttributes.addFlashAttribute("success", "Dữ liệu chương trình đào tạo đã được nhập thành công");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Không thể nhập dữ liệu từ file");
        }

        return "redirect:/training-programs";
    }

    @GetMapping("/enrollment-list")
    public String viewEnrollmentList(Model model) {
        model.addAttribute("enrollments", trainingProgramEnrollmentService.getAllEnrollments());
        model.addAttribute("content", "trainingprograms/enrollment-list");
        return Constants.LAYOUT;
    }
    @GetMapping("/list")
    public String listTrainingProgramsStandalone(Model model,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String searchTerm,
                                                 @RequestParam(required = false) Long classId,
                                                 @RequestParam(required = false) String className) {
        Page<TrainingProgram> trainingPrograms = trainingProgramService.getAllTrainingPrograms(searchTerm, page, size);

        model.addAttribute("trainingPrograms", trainingPrograms);
        model.addAttribute("classId", classId);
        model.addAttribute("className", className);

        // 👇 Thêm đoạn này để map courseId với trạng thái đã đăng ký của class
        if (classId != null) {
            Map<Long, Boolean> enrolledCoursesByClass = new HashMap<>();
            List<User> classStudents = classService.getClassById(classId).getStudents();

            for (TrainingProgram tp : trainingPrograms.getContent()) {
                for (Syllabus s : tp.getSyllabuses()) {
                    for (CourseSyllabus cs : s.getCourseSyllabuses()) {
                        Long courseId = cs.getCourse().getId();
                        boolean anyEnrolled = courseEnrollmentService.isAnyStudentEnrolled(courseId, classStudents);
                        enrolledCoursesByClass.put(courseId, anyEnrolled);
                    }
                }
            }

            model.addAttribute("enrolledCoursesByClass", enrolledCoursesByClass);
        }

        model.addAttribute("content", "trainingprograms/list-standalone");
        return Constants.LAYOUT;
    }
}
