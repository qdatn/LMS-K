package com.example.hcm25_cpl_ks_java_01_lms.modulegroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.dto.ModuleGroupDTO;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModuleGroupService {

    @Autowired
    private ModuleGroupRepository moduleGroupRepository;

    public List<ModuleGroup> getAllModuleGroups() {
        return moduleGroupRepository.findAll();
    }

    public Page<ModuleGroup> getAllModuleGroups(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (searchTerm != null && !searchTerm.isEmpty()) {
            return moduleGroupRepository.findByNameContainingIgnoreCase(searchTerm, pageable);
        }
        return moduleGroupRepository.findAll(pageable);
    }

    public ModuleGroup getModuleGroupById(Long id) {
        return moduleGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ModuleGroup not found with id: " + id));
    }

    public void saveAll(List<ModuleGroup> moduleGroups) {
        moduleGroupRepository.saveAll(moduleGroups);
    }

    public void createModuleGroup(ModuleGroup moduleGroup) {
        if(moduleGroupRepository.existsByName(moduleGroup.getName()))
            throw new ModuleGroupExistException(String.format("Module Group with name '%s' already exists", moduleGroup.getName()));
        moduleGroup.setName(moduleGroup.getName());
        moduleGroupRepository.save(moduleGroup);
    }

    @Transactional
    public void updateModuleGroup(ModuleGroup moduleGroup) {
        ModuleGroup existingModuleGroup = moduleGroupRepository.findByName(moduleGroup.getName());
        if(existingModuleGroup != null && !existingModuleGroup.getId().equals(moduleGroup.getId())) {
            throw new ModuleGroupExistException(String.format("Module Group with name '%s' already exists", moduleGroup.getName()));
        }
        ModuleGroup savedmoduleGroup = moduleGroupRepository.findById(moduleGroup.getId()).orElse(null);
        if(savedmoduleGroup != null) {
            savedmoduleGroup.setModules(new ArrayList<>(savedmoduleGroup.getModules()));
            savedmoduleGroup.setName(moduleGroup.getName());
            moduleGroupRepository.save(savedmoduleGroup);
        }
    }

    public void deleteModuleGroup(Long id) {
        ModuleGroup moduleGroup = getModuleGroupById(id);
        moduleGroupRepository.delete(moduleGroup);
    }

    public ByteArrayInputStream exportToExcel(List<ModuleGroup> moduleGroups) throws IOException {
        return generateExcel(moduleGroups);
    }


    private ByteArrayInputStream generateExcel(List<ModuleGroup> moduleGroups) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("ModuleGroups");

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
            for (ModuleGroup moduleGroup : moduleGroups) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(moduleGroup.getId());
                row.createCell(1).setCellValue(moduleGroup.getName());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public long count() {
        return moduleGroupRepository.count();
    }

    public void deleteModuleGroups(List<Long> ids) {
        moduleGroupRepository.deleteAllById(ids);
    }

    public void deleteAll() {
        moduleGroupRepository.deleteAll();
    }


    //API
    @Transactional(readOnly = true)
    public List<ModuleGroupDTO> getAllModuleGroupsApi() {
        return moduleGroupRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ModuleGroupDTO getModuleGroupByIdApi(Long id) {
        return moduleGroupRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new EntityNotFoundException("ModuleGroup not found with id: " + id));
    }

    @Transactional
    public ModuleGroupDTO createModuleGroupApi(ModuleGroupDTO moduleGroupDTO) {
        ModuleGroup moduleGroup = convertToEntity(moduleGroupDTO);
        return convertToDTO(moduleGroupRepository.save(moduleGroup));
    }

    @Transactional
    public ModuleGroupDTO updateModuleGroupApi(Long id, ModuleGroupDTO moduleGroupDTO) {
        if (!moduleGroupRepository.existsById(id)) {
            throw new EntityNotFoundException("ModuleGroup not found with id: " + id);
        }
        ModuleGroup moduleGroup = convertToEntity(moduleGroupDTO);
        moduleGroup.setId(id);
        return convertToDTO(moduleGroupRepository.save(moduleGroup));
    }

    @Transactional
    public void deleteModuleGroupApi(Long id) {
        if (!moduleGroupRepository.existsById(id)) {
            throw new EntityNotFoundException("ModuleGroup not found with id: " + id);
        }
        moduleGroupRepository.deleteById(id);
    }

    private ModuleGroupDTO convertToDTO(ModuleGroup moduleGroup) {
        ModuleGroupDTO dto = new ModuleGroupDTO();
        dto.setId(moduleGroup.getId());
        dto.setName(moduleGroup.getName());
        return dto;
    }

    private ModuleGroup convertToEntity(ModuleGroupDTO dto) {
        ModuleGroup moduleGroup = new ModuleGroup();
        moduleGroup.setName(dto.getName());
        return moduleGroup;
    }
}
