package com.example.hcm25_cpl_ks_java_01_lms.role.dto;

import com.example.hcm25_cpl_ks_java_01_lms.module.Module;
import com.example.hcm25_cpl_ks_java_01_lms.module.dto.ModuleDTO;
import com.example.hcm25_cpl_ks_java_01_lms.role.Role;
import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {
    private Long id;
    private String name;
    private List<ModuleDTO> modules;

    public Role toEntity() {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }
}
