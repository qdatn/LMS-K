package com.example.hcm25_cpl_ks_java_01_lms.role;

import com.example.hcm25_cpl_ks_java_01_lms.module.dto.ModuleDTO;
import com.example.hcm25_cpl_ks_java_01_lms.role.dto.RoleDTO;
import com.example.hcm25_cpl_ks_java_01_lms.role.exception.ResourceNotFoundException;
import com.example.hcm25_cpl_ks_java_01_lms.role.exception.RoleExistedException;
import com.example.hcm25_cpl_ks_java_01_lms.role.exception.RoleNameTooLongException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import com.example.hcm25_cpl_ks_java_01_lms.module.Module;
import com.example.hcm25_cpl_ks_java_01_lms.module.ModuleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Role')")
public class RoleRestController {

    private final RoleService roleService;
    private final ModuleService moduleService;

    public RoleRestController(RoleService roleService, ModuleService moduleService) {
        this.roleService = roleService;
        this.moduleService = moduleService;
    }

    @GetMapping
    public ResponseEntity<Page<RoleDTO>> getAllRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchTerm) {

        Page<RoleDTO> response = roleService.findAllRoles(searchTerm, page, size).map(Role::toDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable Long id) {
        Role role = roleService.getRoleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return ResponseEntity.ok(role.toDTO());
    }

    @PostMapping
    public ResponseEntity<RoleDTO> createRole(@RequestBody RoleDTO roleDTO) {
        Role savedRole = roleService.createRole(roleDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedRole.toDTO());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDTO> updateRole(
            @PathVariable Long id,
            @RequestBody RoleDTO updatedRoleDetails) {
        if (updatedRoleDetails.getName().length()>=100)
            throw new RoleNameTooLongException("Name can't greater than 100!", HttpStatus.BAD_REQUEST);

        Role existingRole = roleService.getRoleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        boolean nameExists = roleService.getRoleByName(updatedRoleDetails.getName())
                .filter(role -> !role.getId().equals(id))
                .isPresent();
        if (nameExists) {
            throw new RoleExistedException("Role Existed!", HttpStatus.BAD_REQUEST);
        }

        if (existingRole.getName().equals(updatedRoleDetails.getName())) {
            return ResponseEntity.ok(existingRole.toDTO());
        }

        existingRole.setName(updatedRoleDetails.getName());
        Role updatedRole = roleService.save(existingRole);
        return ResponseEntity.ok(updatedRole.toDTO());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteRole(@PathVariable Long id) {
        roleService.getRoleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        
        roleService.deleteRole(id);
        
        return ResponseEntity.ok(true);
    }
    
    @GetMapping("/modules")
    public ResponseEntity<List<ModuleDTO>> getAllModules() {
        List<Module> modules = moduleService.findAll();
        return ResponseEntity.ok(modules.stream().map(Module::toDTO).toList());
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportToExcel(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Role> roles = roleService.findAllRoles("",page, size).getContent();
            ByteArrayInputStream in = roleService.exportToExcel(roles);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=roles.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(new InputStreamResource(in));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/delete-all")
    @Transactional
    public ResponseEntity<String> deleteSelectedRoles(@RequestBody RoleController.DeleteRequest deleteRequest) {
        try {
            List<Long> ids = deleteRequest.getIds();
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("No roles selected for deletion");
            }
            for (Long id : ids) {
                roleService.deleteRole(id);
            }
            return ResponseEntity.ok("Roles deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete roles: " + e.getMessage());
        }
    }

    @PostMapping("/permissions/{id}")
    public ResponseEntity<String> updatePermissions(@PathVariable Long id,
                                    @RequestBody RoleDTO role) {
        try {
            role.setId(id);
            roleService.updateRole(role);
            return ResponseEntity.ok("Permissions updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update permissions: " + e.getMessage());
        }
    }
}