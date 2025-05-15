package com.example.hcm25_cpl_ks_java_01_lms.modulegroup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/module-groups")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Module Groups')")
@Tag(name = "Module Group", description = "Module Group management API")
public class ModuleGroupAPIController {

    private final ModuleGroupService moduleGroupService;

    public ModuleGroupAPIController(ModuleGroupService moduleGroupService) {
        this.moduleGroupService = moduleGroupService;
    }

    @GetMapping
    @Operation(summary = "Get all module groups", description = "Get a paginated list of module groups with optional filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved module groups",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<ModuleGroup>> getAllModuleGroups(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "5") int size,
            @Parameter(description = "Optional search term") @RequestParam(required = false) String searchTerm) {
        try {
            Page<ModuleGroup> moduleGroups = moduleGroupService.getAllModuleGroups(searchTerm, page, size);
            return ResponseEntity.ok(moduleGroups);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get module group by ID", description = "Get module group details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved module group",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ModuleGroup.class))),
            @ApiResponse(responseCode = "404", description = "Module group not found")
    })
    public ResponseEntity<ModuleGroup> getModuleGroupById(
            @Parameter(description = "Module Group ID", required = true) @PathVariable Long id) {
        ModuleGroup moduleGroup = moduleGroupService.getModuleGroupById(id);
        if (moduleGroup != null) {
            return ResponseEntity.ok(moduleGroup);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create a new module group", description = "Create a new module group with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Module group created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ModuleGroup.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Module group name already exists")
    })
    public ResponseEntity<?> createModuleGroup(
            @Parameter(description = "Module group data", required = true) @RequestBody ModuleGroup moduleGroup) {
        try {
            moduleGroupService.createModuleGroup(moduleGroup);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (ModuleGroupExistException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create module group: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a module group", description = "Update an existing module group with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Module group updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ModuleGroup.class))),
            @ApiResponse(responseCode = "404", description = "Module group not found"),
            @ApiResponse(responseCode = "400", description = "Bad request - Module group name already exists")
    })
    public ResponseEntity<?> updateModuleGroup(
            @Parameter(description = "Module Group ID", required = true) @PathVariable Long id,
            @Parameter(description = "Updated module group data", required = true) @RequestBody ModuleGroup moduleGroupDetails) {
        try {
            moduleGroupDetails.setId(id);
            moduleGroupService.updateModuleGroup(moduleGroupDetails);
            return ResponseEntity.ok().build();
        } catch (ModuleGroupExistException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update module group: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a module group", description = "Delete a module group by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Module group deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Module group not found"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<String> deleteModuleGroup(
            @Parameter(description = "Module Group ID", required = true) @PathVariable Long id) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body("Invalid module group ID");
            }
            moduleGroupService.deleteModuleGroup(id);
            return ResponseEntity.ok("Module group deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete module group: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Export module groups to Excel", description = "Export module groups to an Excel file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file generated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> exportToExcel(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        try {
            List<ModuleGroup> moduleGroups = moduleGroupService.getAllModuleGroups("", page, size).getContent();
            ByteArrayInputStream in = moduleGroupService.exportToExcel(moduleGroups);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=modulegroups.xlsx");

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
    @Operation(summary = "Import module groups from Excel", description = "Import module groups from an Excel file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Module groups imported successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid file format or empty file")
    })
    public ResponseEntity<String> importExcel(
            @Parameter(description = "Excel file to import", required = true) @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file to upload");
            }
            if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
                return ResponseEntity.badRequest().body("Only Excel files (.xlsx, .xls) are supported");
            }

            List<ModuleGroup> moduleGroups = ModuleGroupExcelImporter.importModuleGroups(file.getInputStream());
            moduleGroupService.saveAll(moduleGroups);
            return ResponseEntity.ok("File uploaded and data imported successfully");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Could not import data from file: " + e.getMessage());
        }
    }

    @PostMapping("/delete-selected")
    @PreAuthorize("hasAnyRole('Admin')")
    @Operation(summary = "Delete multiple module groups", description = "Delete multiple module groups by IDs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Module groups deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - empty ID list")
    })
    public ResponseEntity<String> deleteModuleGroups(
            @Parameter(description = "List of module group IDs to delete", required = true) @RequestBody DeleteRequest deleteRequest) {
        try {
            List<Long> ids = deleteRequest.getIds();
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("No module groups selected for deletion");
            }
            moduleGroupService.deleteModuleGroups(ids);
            return ResponseEntity.ok("Selected module groups deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete selected module groups: " + e.getMessage());
        }
    }

    @PostMapping("/delete-all")
    @PreAuthorize("hasAnyRole('Admin')")
    @Operation(summary = "Delete all module groups", description = "Delete all module groups")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All module groups deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<String> deleteAllModuleGroups() {
        try {
            long count = moduleGroupService.count();
            if (count == 0) {
                return ResponseEntity.badRequest().body("No module groups to delete");
            }
            moduleGroupService.deleteAll();
            return ResponseEntity.ok(count + " module groups deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete module groups: " + e.getMessage());
        }
    }

    // Inner class for delete request
    public static class DeleteRequest {
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }
}