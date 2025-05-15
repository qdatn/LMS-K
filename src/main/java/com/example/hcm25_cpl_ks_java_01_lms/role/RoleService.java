package com.example.hcm25_cpl_ks_java_01_lms.role;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.hcm25_cpl_ks_java_01_lms.role.dto.RoleDTO;
import com.example.hcm25_cpl_ks_java_01_lms.role.exception.RoleExistedException;
import com.example.hcm25_cpl_ks_java_01_lms.role.exception.RoleNameTooLongException;
import com.example.hcm25_cpl_ks_java_01_lms.role.exception.RoleNotFoundException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hcm25_cpl_ks_java_01_lms.module.Module;
import com.example.hcm25_cpl_ks_java_01_lms.module.ModuleRepository;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    public Role createRole(RoleDTO roleDTO) {
        if (roleDTO.getName().length()>=100)
            throw new RoleNameTooLongException("Name can't greater than 100!", HttpStatus.BAD_REQUEST);

        if (roleRepository.existsByName(roleDTO.getName()))
            throw new RoleExistedException("Role name existed!", HttpStatus.BAD_REQUEST);

        Role role = Role.builder()
                .name(roleDTO.getName())
                .build();
        return roleRepository.save(role);
    }

    public void saveAll(List<Role> roles) {
        roleRepository.saveAll(roles);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                    .orElse(null);

        if(role != null) {
            List<User> users = new ArrayList<>(role.getUsers());
            List<Module> modules = new ArrayList<>(role.getModules());
            
            for (User user : users) {
                user.getRoles().remove(role);
            }
            
            for (Module module : modules) {
                module.getRoles().remove(role);
            }
            
            role.getUsers().clear();
            role.getModules().clear();
            
            roleRepository.save(role);
            roleRepository.delete(role);
        }
    }

    public Page<Role> findAllRoles(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if(searchTerm == null || searchTerm.isEmpty())
            searchTerm = "";
        return roleRepository.findByNameContainingIgnoreCase(searchTerm, pageable);
    }

    public ByteArrayInputStream exportToExcel(List<Role> roles) throws IOException {
        return generateExcel(roles);
    }

    private ByteArrayInputStream generateExcel(List<Role> roles) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Roles");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerCellStyle.setFont(font);

            String[] headers = {"ID", "Name"};
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Create data rows
            int rowIdx = 1;
            for (Role role : roles) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(role.getId());
                row.createCell(1).setCellValue(role.getName());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public Role save(Role role) {
        Role savedRole = roleRepository.save(role);
        
        if (role.getModules() != null) {
            for (Module module : role.getModules()) {
                Module existingModule = moduleRepository.findById(module.getId()).orElse(module);
                List<Role> roles = existingModule.getRoles();
                if (roles == null) roles = new ArrayList<>();
                if (!roles.contains(savedRole)) {
                    roles.add(savedRole);
                }
                existingModule.setRoles(roles);
                moduleRepository.save(existingModule);
            }
        }
        
        return savedRole;
    }

    public void updateRole(RoleDTO role) {
        Role existingRole = roleRepository.findById(role.getId())
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));
            
        if(existingRole != null) {
            if(role.getName() != null && role.getName().length()<=100)
                existingRole.setName(role.getName());
            existingRole.getModules().forEach(module -> module.getRoles().remove(existingRole));
            existingRole.getModules().clear();
            if(role.getModules() != null) {
                existingRole.setModules(role.getModules()
                        .stream()
                        .map(r -> moduleRepository
                                .findByName(r.getName()))
                        .collect(Collectors.toList()));
            }

            roleRepository.save(existingRole);

            existingRole.getModules().forEach(
                    module -> {
                        List<Role> roles = module.getRoles();
                        if (roles == null)
                            roles = new ArrayList<>();
                        roles.add(existingRole);
                        module.setRoles(roles);
                        moduleRepository.save(module);
                    }
            );
        }
    }

    public List<Role> getRolesByModuleName(String moduleName) {
        return roleRepository.findByModuleNameContainingIgnoreCase(moduleName);
    }
}
