package com.example.hcm25_cpl_ks_java_01_lms.module;

import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroup;
import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateModuleDTO {
    private String name;
    private String url;
    private String icon;
    private ModuleGroup moduleGroup;
    private List<Long> roleIds;
}
