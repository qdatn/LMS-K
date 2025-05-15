package com.example.hcm25_cpl_ks_java_01_lms.module.dto;

import com.example.hcm25_cpl_ks_java_01_lms.module.Module;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleDTO {
    private Long id;
    private String name;
    private String icon;
    private String description;
    private Long moduleGroupId;
    private String url;

    public Module toEntity() {
        return Module.builder()
                .id(this.id)
                .name(this.name)
                .url(this.url)
                .description(this.description)
                .icon(this.icon)
                .build();
    }
}
