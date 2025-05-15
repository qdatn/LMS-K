package com.example.hcm25_cpl_ks_java_01_lms.assessmentType;

import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class AssessmentTypeService {

    @Autowired
    private AssessmentTypeRepository assessmentTypeRepository;

    public List<AssessmentType> getAllAssessmentTypes() {
        return assessmentTypeRepository.findAll();
    }

    public Page<AssessmentType> getAllAssessmentTypes(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (searchTerm != null && !searchTerm.isEmpty()) {
            return assessmentTypeRepository.findByNameContainingIgnoreCase(searchTerm, pageable);
        }
        return assessmentTypeRepository.findAll(pageable);
    }

    public void saveAll(List<AssessmentType> assessmentTypes) {
        assessmentTypeRepository.saveAll(assessmentTypes);
    }

    @SneakyThrows
    @Transactional
    public AssessmentType createAssessmentType(AssessmentType assessmentType) {
        validateAssessmentType(assessmentType);
        if (assessmentTypeRepository.findByName(assessmentType.getName()).isPresent()) {
            throw new AssessmentTypeAlreadyExistsException("Assessment Type with name '" + assessmentType.getName() + "' already exists");
        }
        return assessmentTypeRepository.save(assessmentType);
    }

    public long countAllAssessmentTypes() {
        return assessmentTypeRepository.count(); // Đếm tổng số dữ liệu
    }

    public boolean isAssessmentTypeNameExists(String assessmentTypeName) {
        return assessmentTypeRepository.findByName(assessmentTypeName).isPresent();
    }

    @SneakyThrows
    @Transactional
    public AssessmentType updateAssessmentType(AssessmentType assessmentTypeDetails) {
        validateAssessmentType(assessmentTypeDetails);
        AssessmentType existing = getAssessmentTypeById(assessmentTypeDetails.getId());
        Optional<AssessmentType> byName = assessmentTypeRepository.findByName(assessmentTypeDetails.getName());
        if (byName.isPresent() && !byName.get().getId().equals(assessmentTypeDetails.getId())) {
            throw new AssessmentTypeAlreadyExistsException("Assessment Type with name '" + assessmentTypeDetails.getName() + "' already exists");
        }
        return assessmentTypeRepository.save(assessmentTypeDetails);
    }

    // Validation method (example implementation)
    private void validateAssessmentType(AssessmentType assessmentType) {
        if (assessmentType == null) {
            throw new IllegalArgumentException("Assessment Type cannot be null");
        }
        if (assessmentType.getName() == null || assessmentType.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Assessment Type name cannot be empty");
        }
    }

    // Existing method for retrieving by ID
    public AssessmentType getAssessmentTypeById(Integer id) {
        return assessmentTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment Type with ID " + id + " not found"));
    }

    public void deleteAssessmentType(Integer id) {
        AssessmentType assessmentType = getAssessmentTypeById(id);
        assessmentTypeRepository.delete(assessmentType);
    }

    public ByteArrayInputStream exportToExcel(List<AssessmentType> assessmentTypes) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("AssessmentTypes");

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
            for (AssessmentType assessmentType : assessmentTypes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(assessmentType.getId());
                row.createCell(1).setCellValue(assessmentType.getName());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}