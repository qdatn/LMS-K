package com.example.hcm25_cpl_ks_java_01_lms.assessmentType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/assessment-types")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'AssessmentType')")
@Tag(name = "AssessmentType", description = "Assessment Type management API")
public class AssessmentTypeAPIController {

    private final AssessmentTypeService assessmentTypeService;

    public AssessmentTypeAPIController(AssessmentTypeService assessmentTypeService) {
        this.assessmentTypeService = assessmentTypeService;
    }

    @GetMapping
    @Operation(summary = "Get all assessment types", description = "Get a paginated list of assessment types with optional filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assessment types",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<AssessmentType>> listAssessmentTypes(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Optional search term") @RequestParam(required = false) String searchTerm) {
        Page<AssessmentType> assessmentTypes = assessmentTypeService.getAllAssessmentTypes(searchTerm, page, size);
        return ResponseEntity.ok(assessmentTypes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assessment type by ID", description = "Get assessment type details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assessment type",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssessmentType.class))),
            @ApiResponse(responseCode = "404", description = "Assessment type not found")
    })
    public ResponseEntity<AssessmentType> getAssessmentTypeById(
            @Parameter(description = "Assessment Type ID", required = true) @PathVariable Integer id) {
        AssessmentType assessmentType = assessmentTypeService.getAssessmentTypeById(id);
        if (assessmentType != null) {
            return ResponseEntity.ok(assessmentType);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create assessment type", description = "Create a new assessment type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Assessment type created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssessmentType.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - validation error")
    })
    public ResponseEntity<?> createAssessmentType(
            @Parameter(description = "Assessment type data", required = true) @RequestBody AssessmentType assessmentType) {
        try {
            if (assessmentType.getName() == null || assessmentType.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Assessment Type name cannot be empty");
            }
            if (assessmentTypeService.isAssessmentTypeNameExists(assessmentType.getName())) {
                return ResponseEntity.badRequest().body("Assessment Type name already exists!");
            }
            AssessmentType savedAssessmentType = assessmentTypeService.createAssessmentType(assessmentType);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAssessmentType);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update assessment type", description = "Update an existing assessment type by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment type updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssessmentType.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - validation error"),
            @ApiResponse(responseCode = "404", description = "Assessment type not found")
    })
    public ResponseEntity<?> updateAssessmentType(
            @Parameter(description = "Assessment Type ID", required = true) @PathVariable Integer id,
            @Parameter(description = "Updated assessment type data", required = true) @RequestBody AssessmentType assessmentTypeDetails) {
        try {
            AssessmentType existingAssessmentType = assessmentTypeService.getAssessmentTypeById(id);
            if (existingAssessmentType == null) {
                return ResponseEntity.notFound().build();
            }
            if (assessmentTypeDetails.getName() == null || assessmentTypeDetails.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Assessment Type name cannot be empty");
            }
            if (assessmentTypeService.isAssessmentTypeNameExists(assessmentTypeDetails.getName()) &&
                    !assessmentTypeDetails.getName().equals(existingAssessmentType.getName())) {
                return ResponseEntity.badRequest().body("Assessment Type name already exists!");
            }
            assessmentTypeDetails.setId(id);
            AssessmentType updatedAssessmentType = assessmentTypeService.updateAssessmentType(assessmentTypeDetails);
            return ResponseEntity.ok(updatedAssessmentType);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete assessment type", description = "Delete an assessment type by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Assessment type deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Assessment type not found")
    })
    public ResponseEntity<?> deleteAssessmentType(
            @Parameter(description = "Assessment Type ID", required = true) @PathVariable Integer id) {
        try {
            AssessmentType assessmentType = assessmentTypeService.getAssessmentTypeById(id);
            if (assessmentType == null) {
                return ResponseEntity.notFound().build();
            }
            assessmentTypeService.deleteAssessmentType(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/delete-all")
    @Operation(summary = "Delete multiple assessment types", description = "Delete multiple assessment types by IDs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment types deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - no assessment types selected")
    })
    public ResponseEntity<?> deleteSelectedAssessmentTypes(
            @Parameter(description = "List of assessment type IDs", required = true) @RequestBody List<Integer> ids) {
        try {
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

    @GetMapping("/export")
    @Operation(summary = "Export assessment types to Excel", description = "Export assessment types to Excel file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file generated successfully",
                    content = @Content(mediaType = "application/vnd.ms-excel")),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> exportToExcel(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        try {
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
    @PreAuthorize("hasAnyRole('Admin')")
    @Operation(summary = "Import assessment types from Excel", description = "Import assessment types from an Excel file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment types imported successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid file format or empty file")
    })
    public ResponseEntity<?> importExcel(
            @Parameter(description = "Excel file to import", required = true) @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }
        if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
            return ResponseEntity.badRequest().body("Only Excel files (.xlsx, .xls) are supported");
        }
        try {
            List<AssessmentType> assessmentTypes = AssessmentTypeExcelImporter.importAssessmentTypes(file.getInputStream());
            assessmentTypeService.saveAll(assessmentTypes);
            return ResponseEntity.ok("Successfully uploaded and imported data");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to import data from file: " + e.getMessage());
        }
    }

    @GetMapping("/download-template")
    @Operation(summary = "Download template Excel file", description = "Download an Excel template for assessment types import")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template downloaded successfully",
                    content = @Content(mediaType = "application/vnd.ms-excel")),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> downloadExcelTemplate() {
        try {
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