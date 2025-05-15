package com.example.hcm25_cpl_ks_java_01_lms.modulegroup;

import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
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
import java.util.List;

@Controller
@RequestMapping("/module-groups")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Module Groups')")
public class ModuleGroupController {

    private final ModuleGroupService moduleGroupService;

    public ModuleGroupController(ModuleGroupService moduleGroupService) {
        this.moduleGroupService = moduleGroupService;
    }


    @GetMapping
    public String getAllModuleGroups(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "5") int size,
                                   @RequestParam(required = false) String searchTerm) {
        Page<ModuleGroup> moduleGroups = moduleGroupService.getAllModuleGroups(searchTerm, page, size);
        model.addAttribute("moduleGroups", moduleGroups);
        model.addAttribute("content", "modulegroups/list");
        return Constants.LAYOUT;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("moduleGroup", new ModuleGroup());
        model.addAttribute("content", "modulegroups/create");
        return Constants.LAYOUT;
    }

    @PostMapping
    public String createModuleGroup(@ModelAttribute ModuleGroup moduleGroup, Model model) {
        try {
            moduleGroupService.createModuleGroup(moduleGroup);
            return "redirect:/module-groups";
        } catch (ModuleGroupExistException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("moduleGroup", moduleGroup);
            model.addAttribute("content", "modulegroups/create");
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ModuleGroup moduleGroup = moduleGroupService.getModuleGroupById(id);
        if (moduleGroup != null) {
            model.addAttribute("moduleGroup", moduleGroup);
            model.addAttribute("content", "modulegroups/update");
            return Constants.LAYOUT;
        }
        return "redirect:/module-groups";
    }

    @PostMapping("/edit/{id}")
    public String updateModuleGroup(@PathVariable Long id, @ModelAttribute ModuleGroup moduleGroupDetails, Model model) {
        try {
            moduleGroupDetails.setId(id);
            moduleGroupService.updateModuleGroup(moduleGroupDetails);
            return "redirect:/module-groups";
        } catch (ModuleGroupExistException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("moduleGroup", moduleGroupDetails);
            model.addAttribute("content", "modulegroups/update");
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteModuleGroup(@PathVariable Long id) {
        moduleGroupService.deleteModuleGroup(id);
        return "redirect:/module-groups";
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportToExcel(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
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
    public String importExcel(@RequestParam("file") MultipartFile file, Model model) {
        model.addAttribute("content", "modulegroups/list");
        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a file to upload");
            return Constants.LAYOUT;
        }

        try {
            List<ModuleGroup> moduleGroups = ModuleGroupExcelImporter.importModuleGroups(file.getInputStream());
            moduleGroupService.saveAll(moduleGroups);
            model.addAttribute("success", "File uploaded and data imported successfully");
            return "redirect:/module-groups";
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Could not import data from file");
            return Constants.LAYOUT;
        }
    }

    @PostMapping("/delete-selected")
    @PreAuthorize("hasAnyRole('Admin')")
    public String deleteActivities(@RequestParam("ids") List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return "redirect:/activities?error=No activities selected for deletion";
            }
            moduleGroupService.deleteModuleGroups(ids);
            return "redirect:/module-groups?success=Selected module groups deleted successfully";
        } catch (Exception e) {
            return "redirect:/module-groups?error=Failed to delete selected module groups: " + e.getMessage();
        }
    }

    @PostMapping("/delete-all")
    @PreAuthorize("hasAnyRole('Admin')")
    public String deleteAllActivities() {
        try {
            long count = moduleGroupService.count();
            if (count == 0) {
                return "redirect:/module-groups?error=No module groups to delete";
            }
            moduleGroupService.deleteAll();
            return "redirect:/module-groups?success=" + count + " module groups deleted successfully";
        } catch (Exception e) {
            return "redirect:/module-groups?error=Failed to delete module groups: " + e.getMessage();
        }
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('Admin')")
    public String deleteActivity(@PathVariable Long id) {
        try {
            if (id == null) {
                return "redirect:/activities?error=Invalid activity ID";
            }
            moduleGroupService.deleteModuleGroup(id);
            return "redirect:/module-groups?success=Module group deleted successfully";
        } catch (Exception e) {
            return "redirect:/module-groups?error=Failed to delete module group: " + e.getMessage();
        }
    }
}
