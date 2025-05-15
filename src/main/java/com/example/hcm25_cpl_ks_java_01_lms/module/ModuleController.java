package com.example.hcm25_cpl_ks_java_01_lms.module;

import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroup;
import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroupService;
import com.example.hcm25_cpl_ks_java_01_lms.role.Role;
import com.example.hcm25_cpl_ks_java_01_lms.role.RoleService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/modules")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Modules')")
public class ModuleController {

    private final ModuleService moduleService;

    private final RoleService roleService;

    private final ModuleGroupService moduleGroupService;

    public ModuleController(ModuleService moduleService, RoleService roleService,
            ModuleGroupService moduleGroupService) {
        this.moduleService = moduleService;
        this.roleService = roleService;
        this.moduleGroupService = moduleGroupService;
    }

    @GetMapping
    public String getAllModules(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String searchTerm) {
        Page<Module> modules = moduleService.getAllModules(page, size, searchTerm);
        model.addAttribute("modules", modules);
        model.addAttribute("content", "modules/list");
        return Constants.LAYOUT;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        List<ModuleGroup> moduleGroups = moduleGroupService.getAllModuleGroups();
        List<Role> roles = roleService.getAllRoles();
        model.addAttribute("moduleGroups", moduleGroups);
        model.addAttribute("roles", roles);
        model.addAttribute("module", new CreateModuleDTO());
        model.addAttribute("content", "modules/create");
        return Constants.LAYOUT;
    }

    @PostMapping
    public String createModule(@ModelAttribute CreateModuleDTO module, Model model) {
        Module createdModule = moduleService.createModule(module);
        if (createdModule != null) {
            model.addAttribute("module", createdModule);
            return "redirect:/modules";
        } else {
            List<ModuleGroup> moduleGroups = moduleGroupService.getAllModuleGroups();
            List<Role> roles = roleService.getAllRoles();
            model.addAttribute("error", "Name or path already exists!");

            model.addAttribute("moduleGroups", moduleGroups);
            model.addAttribute("roles", roles);
            model.addAttribute("module", module);
            model.addAttribute("content", "modules/create");
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Module module = moduleService.getModuleById(id);
        if (module != null) {
            loadEditModuleContent(model);
            model.addAttribute("module", module);
            return Constants.LAYOUT;
        }
        return "redirect:/modules";
    }

    @PostMapping("/edit/{id}")
    public String updateModule(@PathVariable Long id, @ModelAttribute Module moduleDetails, Model model) {
        moduleDetails.setId(id);
        Module updatedModule = moduleService.updateModule(moduleDetails);
        if (updatedModule != null) {
            model.addAttribute("module", updatedModule);
            return "redirect:/modules";
        } else {
            loadEditModuleContent(model);
            model.addAttribute("module", moduleDetails);
            model.addAttribute("error", "Name or path already exists!");
            return Constants.LAYOUT;
        }
    }

    public void loadEditModuleContent(Model model) {
        List<Role> roles = roleService.getAllRoles();
        List<ModuleGroup> moduleGroups = moduleGroupService.getAllModuleGroups();
        model.addAttribute("roles", roles);
        model.addAttribute("moduleGroups", moduleGroups);
        model.addAttribute("content", "modules/update");
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportToExcel(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
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
    public String importExcel(@RequestParam("file") MultipartFile file, Model model) {
        model.addAttribute("content", "modules/list");
        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a file to upload");
            return Constants.LAYOUT;
        }

        try {
            List<Module> modules = ModuleExcelImporter.importModules(file.getInputStream());
            moduleService.saveAll(modules);
            model.addAttribute("success", "File uploaded and data imported successfully");
            return "redirect:/modules";
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Could not import data from file");
            return Constants.LAYOUT;
        }
    }

    @PostMapping("/delete-selected")
    @PreAuthorize("hasAnyRole('Admin')")
    public String deleteModules(@RequestParam("ids") List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return "redirect:/modules?error=No modules selected for deletion";
            }
            moduleService.deleteModules(ids);
            return "redirect:/modules?success=Selected modules deleted successfully";
        } catch (Exception e) {
            return "redirect:/modules?error=Failed to delete selected modules: " + e.getMessage();
        }
    }

   @PostMapping("/delete-all")
    @PreAuthorize("hasAnyRole('Admin')")
    public String deleteAllModules() {
        try {
            long count = moduleService.count();
            if (count == 0) {
                return "redirect:/modules?error=No modules to delete";
            }
            moduleService.deleteAll();
            return "redirect:/modules?success=" + count + " modules deleted successfully";
        } catch (Exception e) {
            return "redirect:/modules?error=Failed to delete modules: " + e.getMessage();
        }
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('Admin')")
    public String deleteModule(@PathVariable Long id) {
        try {
            if (id == null) {
                return "redirect:/modules?error=Invalid module ID";
            }
            moduleService.deleteModule(id);
            return "redirect:/modules?success=Module deleted successfully";
        } catch (Exception e) {
            return "redirect:/modules?error=Failed to delete module: " + e.getMessage();
        }
    }

    @GetMapping("/download-template")
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

}
