package com.example.hcm25_cpl_ks_java_01_lms.module;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.example.hcm25_cpl_ks_java_01_lms.module.dto.ModuleDTO;
import jakarta.persistence.EntityNotFoundException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroup;
import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroupRepository;
import com.example.hcm25_cpl_ks_java_01_lms.role.Role;
import com.example.hcm25_cpl_ks_java_01_lms.role.RoleRepository;

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final ModuleGroupRepository moduleGroupRepository;

    private final RoleRepository roleRepository;

    public ModuleService(ModuleRepository moduleRepository, ModuleGroupRepository moduleGroupRepository,
            RoleRepository roleRepository) {
        this.moduleRepository = moduleRepository;
        this.moduleGroupRepository = moduleGroupRepository;
        this.roleRepository = roleRepository;
    }

    public Module createModule(CreateModuleDTO moduleDTO) {
        if (!moduleRepository.existsByNameOrUrl(moduleDTO.getName(), moduleDTO.getUrl())) {
            Module module = Module.builder()
                    .name(moduleDTO.getName())
                    .icon(moduleDTO.getIcon())
                    .url(moduleDTO.getUrl())
                    .moduleGroup(moduleDTO.getModuleGroup())
                    .build();
            return moduleRepository.save(module);
        } else
            return null;
    }

    public Module updateModule(Module moduleDetails) {
        Module existingModuleByName = moduleRepository.findByName(moduleDetails.getName());
        if (existingModuleByName != null && !existingModuleByName.getId().equals(moduleDetails.getId())) {
            return null;
        }

        Module existingModuleByUrl = moduleRepository.findByUrl(moduleDetails.getUrl());
        if (existingModuleByUrl != null && !existingModuleByUrl.getId().equals(moduleDetails.getId())) {
            return null;
        }

        return moduleRepository.save(moduleDetails);
    }

    @Transactional
    public void deleteModule(Long id) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found with id: " + id));

        List<Role> roles = module.getRoles();
        if (roles != null && !roles.isEmpty()) {
            roles.forEach(role -> {
                if (role.getModules().remove(module))
                    roleRepository.save(role);
            });
        }

        moduleRepository.deleteById(id);
    }

    public Page<Module> getAllModules(int page, int size, String searchTerm) {
        Pageable pageable = PageRequest.of(page, size);
        if (searchTerm != null && !searchTerm.isEmpty()) {
            return moduleRepository.findByNameContainingIgnoreCase(searchTerm, pageable);
        }
        return moduleRepository.findAll(pageable);
    }

    public Module getModuleById(Long id) {
        return moduleRepository.findById(id).orElse(null);
    }

    public ByteArrayInputStream exportToExcel(List<Module> modules) throws IOException {
        return generateExcel(modules);
    }

    private ByteArrayInputStream generateExcel(List<Module> modules) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Modules");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerCellStyle.setFont(font);

            String[] headers = { "ID", "Name", "URL", "Icon", "Module Group" };
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Create data rows
            int rowIdx = 1;
            for (Module module : modules) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(module.getId());
                row.createCell(1).setCellValue(module.getName());
                row.createCell(2).setCellValue(module.getUrl());
                row.createCell(3).setCellValue(module.getIcon());
                row.createCell(4).setCellValue(module.getModuleGroup().getName());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public void saveAll(List<Module> modules) {
        for (Module m : modules) {
            ModuleGroup moduleGroup = moduleGroupRepository.getByName(m.getModuleGroup().getName());
            m.setModuleGroup(moduleGroup);
        }
        moduleRepository.saveAll(modules);
    }

    public List<Module> findAll() {
        return moduleRepository.findAll();
    }

    public List<Module> getModulesByRole(String role) {
        if (role.equals("Admin"))
            return moduleRepository.findAll();
        return moduleRepository.findByRoles_Name(role);
    }

    public long count() {
        return moduleRepository.count();
    }

    public void deleteAll() {
        moduleRepository.deleteAll();
    }

    public void deleteModules(List<Long> ids) {
        moduleRepository.deleteAllById(ids);
    }

    //API
    @Transactional(readOnly = true)
    public List<ModuleDTO> getAllModulesApi() {
        return moduleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ModuleDTO getModuleByIdApi(Long id) {
        return moduleRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Module not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ModuleDTO> getModulesByGroupIdApi(Long moduleGroupId) {
        return moduleRepository.findByModuleGroupId(moduleGroupId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ModuleDTO createModuleApi(ModuleDTO moduleDTO) {
        ModuleGroup moduleGroup = moduleGroupRepository.findById(moduleDTO.getModuleGroupId())
                .orElseThrow(() -> new EntityNotFoundException("ModuleGroup not found with id: " + moduleDTO.getModuleGroupId()));

        Module module = convertToEntity(moduleDTO);
        module.setModuleGroup(moduleGroup);
        return convertToDTO(moduleRepository.save(module));
    }

    @Transactional
    public ModuleDTO updateModuleApi(Long id, ModuleDTO moduleDTO) {
        if (!moduleRepository.existsById(id)) {
            throw new EntityNotFoundException("Module not found with id: " + id);
        }

        ModuleGroup moduleGroup = moduleGroupRepository.findById(moduleDTO.getModuleGroupId())
                .orElseThrow(() -> new EntityNotFoundException("ModuleGroup not found with id: " + moduleDTO.getModuleGroupId()));

        Module module = convertToEntity(moduleDTO);
        module.setId(id);
        module.setModuleGroup(moduleGroup);
        return convertToDTO(moduleRepository.save(module));
    }

    @Transactional
    public void deleteModuleApi(Long id) {
        if (!moduleRepository.existsById(id)) {
            throw new EntityNotFoundException("Module not found with id: " + id);
        }
        moduleRepository.deleteById(id);
    }

    private ModuleDTO convertToDTO(Module module) {
        ModuleDTO dto = new ModuleDTO();
        dto.setId(module.getId());
        dto.setName(module.getName());
        dto.setDescription(module.getDescription());
        dto.setModuleGroupId(module.getModuleGroup().getId());
        return dto;
    }

    private Module convertToEntity(ModuleDTO dto) {
        Module module = new Module();
        module.setName(dto.getName());
        module.setDescription(dto.getDescription());
        return module;
    }
}
