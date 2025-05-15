package com.example.hcm25_cpl_ks_java_01_lms.main;

import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import com.example.hcm25_cpl_ks_java_01_lms.module.Module;
import com.example.hcm25_cpl_ks_java_01_lms.module.ModuleService;
import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroup;
import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroupService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/")
public class MainController{

    private final ModuleService moduleService;

    private final ModuleGroupService moduleGroupService;

    public MainController(ModuleService moduleService, ModuleGroupService moduleGroupService) {
        this.moduleService = moduleService;
        this.moduleGroupService = moduleGroupService;
    }

    @GetMapping({ "", "dashboard" })
    public String home(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().iterator().next().getAuthority().substring(5);

        List<Module> modules = moduleService.getModulesByRole(role);
        model.addAttribute("modules", modules);
        model.addAttribute("content", "dashboard");
        return Constants.LAYOUT;
    }


}