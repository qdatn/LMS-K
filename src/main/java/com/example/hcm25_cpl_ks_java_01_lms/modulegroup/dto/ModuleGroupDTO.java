package com.example.hcm25_cpl_ks_java_01_lms.modulegroup.dto;


import com.example.hcm25_cpl_ks_java_01_lms.module.dto.ModuleDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleGroupDTO {
    private Long id;
    private String name;
    private String description;
    private List<ModuleDTO> modules;
}
