package com.example.hcm25_cpl_ks_java_01_lms.module.mapper;

import com.example.hcm25_cpl_ks_java_01_lms.module.Module;
import com.example.hcm25_cpl_ks_java_01_lms.module.dto.ModuleDTO;
import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroup;
import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModuleMapper {
    @Autowired
    private ModuleGroupRepository moduleGroupRepository;

    public ModuleDTO toDto(Module module) {
        ModuleDTO dto = new ModuleDTO();
        dto.setId(module.getId());
        dto.setName(module.getName());
        dto.setDescription(module.getDescription());
        dto.setModuleGroupId(module.getModuleGroup().getId());
        dto.setUrl(module.getUrl());
        return dto;
    }

    public Module toEntity(ModuleDTO dto) {
        Module module = new Module();
        module.setId(dto.getId());
        module.setName(dto.getName());
        module.setDescription(dto.getDescription());
        module.setUrl(dto.getUrl());

        // Fetch and set the ModuleGroup
        ModuleGroup moduleGroup = moduleGroupRepository.findById(dto.getModuleGroupId())
                .orElseThrow(() -> new RuntimeException("ModuleGroup not found with id: " + dto.getModuleGroupId()));
        module.setModuleGroup(moduleGroup);

        return module;
    }
}