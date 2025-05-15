package com.example.hcm25_cpl_ks_java_01_lms.training_program_enrollment;

import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/training-program-enrollments")
public class TrainingProgramEnrollmentController {

    private final TrainingProgramEnrollmentService enrollmentService;

    @Autowired
    public TrainingProgramEnrollmentController(TrainingProgramEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public String listEnrollments(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String searchTerm) {
        Page<TrainingProgramEnrollment> enrollmentsPage = enrollmentService.getAllEnrollments(searchTerm, page, size);

        if (enrollmentsPage.isEmpty()) {
            model.addAttribute("error", "No enrollments found.");
        }

        model.addAttribute("enrollments", enrollmentsPage); // Đảm bảo biến này trùng với Thymeleaf
        model.addAttribute("content", "trainingprogramenrollments/list");

        return Constants.LAYOUT;
    }


    @PostMapping("/delete/{id}")
    public String deleteEnrollment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            enrollmentService.deleteEnrollmentById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Enrollment deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting enrollment: " + e.getMessage());
        }
        return "redirect:/training-programs/enrollment-list";
    }

    @PostMapping("/delete-selected")
    public ResponseEntity<?> deleteSelected(@RequestBody Map<String, List<Long>> payload) {
        List<Long> ids = payload.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("No IDs provided");
        }

        enrollmentService.deleteTrainingProgramsByIds(ids);
        return ResponseEntity.ok().body("Deleted successfully");
    }
}