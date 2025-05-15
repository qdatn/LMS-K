package com.example.hcm25_cpl_ks_java_01_lms.assessmentType;

import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/assessment-types")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'AssessmentType')")
public class AssessmentTypeController {

    private final AssessmentTypeService assessmentTypeService;

    public AssessmentTypeController(AssessmentTypeService assessmentTypeService) {
        this.assessmentTypeService = assessmentTypeService;
    }

    @GetMapping
    public String getAllAssessmentTypes(Model model,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String searchTerm) {
        try {
            Page<AssessmentType> assessmentTypes = assessmentTypeService.getAllAssessmentTypes(searchTerm, page, size);
            model.addAttribute("assessmentTypes", assessmentTypes);
            model.addAttribute("content", "assessment_types/list");
            return Constants.LAYOUT;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("assessmentType", new AssessmentType());
        model.addAttribute("content", "assessment_types/create");
        return Constants.LAYOUT;
    }

    @PostMapping
    public String createAssessmentType(@ModelAttribute AssessmentType assessmentType, Model model) {
        try {
            if (assessmentTypeService.isAssessmentTypeNameExists(assessmentType.getName())) {
                model.addAttribute("error", "Assessment Type name already exists!");
                model.addAttribute("assessmentType", assessmentType);
                model.addAttribute("content", "assessment_types/create");
                return Constants.LAYOUT;
            }
            assessmentTypeService.createAssessmentType(assessmentType);
            return "redirect:/assessment-types";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("assessmentType", assessmentType);
            model.addAttribute("content", "assessment_types/create");
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        AssessmentType assessmentType = assessmentTypeService.getAssessmentTypeById(id);
        if (assessmentType != null) {
            model.addAttribute("assessmentType", assessmentType);
            model.addAttribute("content", "assessment_types/edit");
            return Constants.LAYOUT;
        }
        return "redirect:/assessment-types";
    }

    @PostMapping("/edit/{id}")
    public String updateAssessmentType(@PathVariable Integer id, @ModelAttribute AssessmentType assessmentTypeDetails, Model model) {
        try {
            if (assessmentTypeService.isAssessmentTypeNameExists(assessmentTypeDetails.getName())) {
                model.addAttribute("error", "Assessment Type name already exists!");
                model.addAttribute("assessmentType", assessmentTypeDetails);
                model.addAttribute("content", "assessment_types/edit");
                return Constants.LAYOUT;
            }
            assessmentTypeDetails.setId(id);
            assessmentTypeService.updateAssessmentType(assessmentTypeDetails);
            return "redirect:/assessment-types";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("assessmentType", assessmentTypeDetails);
            model.addAttribute("content", "assessment_types/edit");
            return Constants.LAYOUT;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteAssessmentType(@PathVariable Integer id) {
        assessmentTypeService.deleteAssessmentType(id);
        return "redirect:/assessment-types";
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportToExcel(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(required = false) Integer size) {
        try {
            // Nếu size không được truyền vào, mặc định lấy toàn bộ dữ liệu
            if (size == null) {
                size = (int) assessmentTypeService.countAllAssessmentTypes();
            }
            List<AssessmentType> assessmentTypes = assessmentTypeService.getAllAssessmentTypes("", page, size).getContent();
            ByteArrayInputStream in = assessmentTypeService.exportToExcel(assessmentTypes);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=assessment_types.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(new InputStreamResource(in));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
            List<AssessmentType> assessmentTypes = AssessmentTypeExcelImporter.importAssessmentTypes(file.getInputStream());
            assessmentTypeService.saveAll(assessmentTypes);
            return "redirect:/assessment-types";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to import: " + e.getMessage());
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/print")
    public String printAssessmentTypes(Model model) {
        model.addAttribute("assessmentTypes", assessmentTypeService.getAllAssessmentTypes("", 0, Integer.MAX_VALUE));
        return "assessment_types/print";
    }

    // Class để nhận dữ liệu từ request body
    public static class DeleteRequest {
        private List<Integer> ids;

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }
    }

    @PostMapping("/delete-all")
    @Transactional
    public ResponseEntity<String> deleteSelectedAssessmentTypes(@RequestBody AssessmentTypeController.DeleteRequest deleteRequest, Model model) {
        try {
            List<Integer> ids = deleteRequest.getIds();
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("No assessment types selected for deletion");
            }
            for (Integer id : ids) {
                assessmentTypeService.deleteAssessmentType(id);
            }
            return ResponseEntity.ok("Assessment types deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete assessment types: " + e.getMessage());
        }
    }

    @GetMapping("/download-template")
    public ResponseEntity<Resource> downloadExcelTemplate() {
        try {
            // Đường dẫn tương đối từ thư mục gốc của project
            Path filePath = Paths.get("data-excel/assessment_type_template.xlsx");
            Resource resource = new ByteArrayResource(Files.readAllBytes(filePath));

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=assessment_type_template.xlsx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
