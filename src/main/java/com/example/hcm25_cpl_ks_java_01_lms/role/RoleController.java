package com.example.hcm25_cpl_ks_java_01_lms.role;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.hcm25_cpl_ks_java_01_lms.role.dto.RoleDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import com.example.hcm25_cpl_ks_java_01_lms.module.Module;
import com.example.hcm25_cpl_ks_java_01_lms.module.ModuleService;

import lombok.Getter;
import lombok.Setter;

@Controller
@RequestMapping("/roles")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Role')")
public class RoleController {

    private final RoleService roleService;

    private final ModuleService moduleService;

    public RoleController(RoleService roleService, ModuleService moduleService) {
        this.roleService = roleService;
        this.moduleService = moduleService;
    }

    @GetMapping
    public String listRoles(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String searchTerm) {
        Page<Role> roles = roleService.findAllRoles(searchTerm, page, size);
        model.addAttribute("roles", roles);
        model.addAttribute("content", "roles/list");
        return Constants.LAYOUT;
    }

    // Display a single role by ID
    @GetMapping("/{id}") //details: ==> roles/3
    public String getRoleById(@PathVariable Long id, Model model) {
        Optional<Role> role = roleService.getRoleById(id);
        if (role.isPresent()) {
            model.addAttribute("role", role.get());
            model.addAttribute("content", "roles/detail");
            return Constants.LAYOUT;
        } else {
            return "redirect:/roles";
        }
    }

    // Display the form to create a new role
    @GetMapping("/new") //roles/new
    public String showCreateForm(Model model) {
        List<Module> modules = moduleService.findAll();

        model.addAttribute("modules", modules);
        model.addAttribute("role", new RoleDTO());
        model.addAttribute("content", "roles/create");
        return Constants.LAYOUT;
    }

    // Display the form to edit an existing role
    @GetMapping("/edit/{id}") //roles/edit/2
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Role> role = roleService.getRoleById(id);
        if (role.isPresent()) {
            List<Module> modules = moduleService.findAll();
            model.addAttribute("modules", modules);
            model.addAttribute("role", role.get());
            model.addAttribute("content", "roles/edit");
            return Constants.LAYOUT;
        } else {
            return "redirect:/roles";
        }
    }



    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file, Model model) {
        model.addAttribute("content", "roles/list");
        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a file to upload");
            return Constants.LAYOUT;
        }

        try {
            List<Role> roles = RoleExcelImporter.importRoles(file.getInputStream());
            roleService.saveAll(roles);
            model.addAttribute("success", "Tải lên file và nhập dữ liệu thành công");
            return "redirect:/roles";
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Không thể nhập dữ liệu từ file");
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/print")
    public String printRole(Model model) {
        model.addAttribute("roles", roleService.findAllRoles("", 0, Integer.MAX_VALUE));
        return "roles/print";
    }

    @GetMapping("/download-template")
    public ResponseEntity<Resource> downloadExcelTemplate() {
        try {
            // Đường dẫn tương đối từ thư mục gốc của project
            Path filePath = Paths.get("data-excel/role_template.xlsx");
            Resource resource = new ByteArrayResource(Files.readAllBytes(filePath));

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=role_template.xlsx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @GetMapping("/permissions/{id}")
    public String showPermissionsForm(@PathVariable Long id, Model model) {
        Optional<Role> role = roleService.getRoleById(id);
        if (role.isPresent()) {
            List<Module> modules = moduleService.findAll();
            List<Module> assignedModules = role.get().getModules();
            List<Module> availableModules = modules.stream()
                    .filter(m -> !assignedModules.contains(m))
                    .collect(Collectors.toList());
            model.addAttribute("modules", availableModules);
            model.addAttribute("role", role.get());
            model.addAttribute("content", "roles/permission");
            return Constants.LAYOUT;
        } else {
            return "redirect:/roles";
        }
    }



    @Getter
    @Setter
    public static class DeleteRequest {
        private List<Long> ids;
    }
}
