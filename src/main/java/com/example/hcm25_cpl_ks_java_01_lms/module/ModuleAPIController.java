package com.example.hcm25_cpl_ks_java_01_lms.module;

import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroup;
import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroupService;
import com.example.hcm25_cpl_ks_java_01_lms.role.Role;
import com.example.hcm25_cpl_ks_java_01_lms.role.RoleService;
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
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/modules")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Modules')")
@Tag(name = "Module", description = "Module management API")
public class ModuleAPIController {

    private final ModuleService moduleService;
    private final RoleService roleService;
    private final ModuleGroupService moduleGroupService;

    public ModuleAPIController(ModuleService moduleService, RoleService roleService,
            ModuleGroupService moduleGroupService) {
        this.moduleService = moduleService;
        this.roleService = roleService;
        this.moduleGroupService = moduleGroupService;
    }

    @GetMapping
    @Operation(summary = "Get all modules", description = "Get a paginated list of modules with optional filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved modules",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<Module>> getAllModules(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "5") int size,
            @Parameter(description = "Optional search term") @RequestParam(required = false) String searchTerm) {
        try {
            Page<Module> modules = moduleService.getAllModules(page, size, searchTerm);
            return ResponseEntity.ok(modules);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get module by ID", description = "Get module details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved module",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Module.class))),
            @ApiResponse(responseCode = "404", description = "Module not found")
    })
    public ResponseEntity<Module> getModuleById(
            @Parameter(description = "Module ID", required = true) @PathVariable Long id) {
        Module module = moduleService.getModuleById(id);
        if (module != null) {
            return ResponseEntity.ok(module);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/module-groups")
    @Operation(summary = "Get all module groups", description = "Get a list of all module groups")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved module groups")
    })
    public ResponseEntity<List<ModuleGroup>> getAllModuleGroups() {
        List<ModuleGroup> moduleGroups = moduleGroupService.getAllModuleGroups();
        return ResponseEntity.ok(moduleGroups);
    }

    @GetMapping("/roles")
    @Operation(summary = "Get all roles", description = "Get a list of all roles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved roles")
    })
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @PostMapping
    @Operation(summary = "Create a new module", description = "Create a new module with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Module created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Module.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Name or path already exists")
    })
    public ResponseEntity<?> createModule(
            @Parameter(description = "Module data", required = true) @RequestBody CreateModuleDTO module) {
        try {
            Module createdModule = moduleService.createModule(module);
            if (createdModule != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(createdModule);
            } else {
                return ResponseEntity.badRequest().body("Name or path already exists!");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create module: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a module", description = "Update an existing module with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Module updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Module.class))),
            @ApiResponse(responseCode = "404", description = "Module not found"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<?> updateModule(
            @Parameter(description = "Module ID", required = true) @PathVariable Long id,
            @Parameter(description = "Updated module data", required = true) @RequestBody Module moduleDetails) {
        try {
            moduleDetails.setId(id);
            Module updatedModule = moduleService.updateModule(moduleDetails);
            if (updatedModule != null) {
                return ResponseEntity.ok(updatedModule);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update module: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin')")
    @Operation(summary = "Delete a module", description = "Delete a module by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Module deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Module not found"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<String> deleteModule(
            @Parameter(description = "Module ID", required = true) @PathVariable Long id) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body("Invalid module ID");
            }
            moduleService.deleteModule(id);
            return ResponseEntity.ok("Module deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete module: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Export modules to Excel", description = "Export modules to an Excel file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file generated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> exportToExcel(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        try {
            List<Module> modules = moduleService.getAllModules(page, size, "").getContent();
            ByteArrayInputStream in = moduleService.exportToExcel(modules);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=modules.xlsx");

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
    @Operation(summary = "Import modules from Excel", description = "Import modules from an Excel file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modules imported successfully"),
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

            List<Module> modules = ModuleExcelImporter.importModules(file.getInputStream());
            moduleService.saveAll(modules);
            return ResponseEntity.ok("File uploaded and data imported successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Could not import data from file: " + e.getMessage());
        }
    }

    @PostMapping("/delete-selected")
    @PreAuthorize("hasAnyRole('Admin')")
    @Operation(summary = "Delete multiple modules", description = "Delete multiple modules by IDs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modules deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - empty ID list")
    })
    public ResponseEntity<String> deleteModules(
            @Parameter(description = "List of module IDs to delete", required = true) @RequestBody DeleteRequest deleteRequest) {
        try {
            List<Long> ids = deleteRequest.getIds();
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("No modules selected for deletion");
            }
            moduleService.deleteModules(ids);
            return ResponseEntity.ok("Selected modules deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete selected modules: " + e.getMessage());
        }
    }

    @PostMapping("/delete-all")
    @PreAuthorize("hasAnyRole('Admin')")
    @Operation(summary = "Delete all modules", description = "Delete all modules")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All modules deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<String> deleteAllModules() {
        try {
            long count = moduleService.count();
            if (count == 0) {
                return ResponseEntity.badRequest().body("No modules to delete");
            }
            moduleService.deleteAll();
            return ResponseEntity.ok(count + " modules deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete modules: " + e.getMessage());
        }
    }

    @GetMapping("/download-template")
    @Operation(summary = "Download Excel template", description = "Download an empty Excel template for module import")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template generated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> downloadExcelTemplate() {
        try {
            List<Module> modules = new ArrayList<>();
            ByteArrayInputStream in = moduleService.exportToExcel(modules);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=modules.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(new InputStreamResource(in));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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