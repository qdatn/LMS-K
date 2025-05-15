package com.example.hcm25_cpl_ks_java_01_lms.role;

import com.example.hcm25_cpl_ks_java_01_lms.module.Module;
import com.example.hcm25_cpl_ks_java_01_lms.module.dto.ModuleDTO;
import com.example.hcm25_cpl_ks_java_01_lms.role.dto.RoleDTO;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "roles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<User> users;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_modules",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "module_id")
    )
    private List<Module> modules;

    @Override
    public String toString() {
        return name;
    }

    public RoleDTO toDTO() {
        if (this.modules == null)
            this.modules = new ArrayList<>();
        List<ModuleDTO> moduleDTOS = this.modules.stream().map(Module::toDTO).toList();
        return RoleDTO.builder()
                .id(this.id)
                .name(this.name)
                .modules(moduleDTOS)
                .build();
    }
    
}
