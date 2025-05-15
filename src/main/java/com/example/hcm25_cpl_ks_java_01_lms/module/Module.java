package com.example.hcm25_cpl_ks_java_01_lms.module;

import com.example.hcm25_cpl_ks_java_01_lms.module.dto.ModuleDTO;
import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroup;
import com.example.hcm25_cpl_ks_java_01_lms.role.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Entity
@Table(name = "modules")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String url;

    private String description;

    @Column(length = 255)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_group_id", nullable = false)
    private ModuleGroup moduleGroup;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;

    @Override
    public String toString() {
        return name;
    }

    public ModuleDTO toDTO() {
        return ModuleDTO.builder()
                .id(this.id)
                .name(this.name)
                .url(this.url)
                .description(this.description)
                .icon(this.icon)
                .moduleGroupId(this.moduleGroup.getId())
                .build();
    }
}
