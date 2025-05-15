package com.example.hcm25_cpl_ks_java_01_lms.assessmentType;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AssessmentTypeExcelImporter {
    public static List<AssessmentType> importAssessmentTypes(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        List<AssessmentType> assessmentTypes = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file contains no sheets");
            }

            Iterator<Row> rows = sheet.iterator();
            if (!rows.hasNext()) {
                throw new IllegalArgumentException("Excel file is empty");
            }

            rows.next(); // Skip header row
            int rowNum = 1;

            while (rows.hasNext()) {
                Row row = rows.next();
                rowNum++;
                try {
                    AssessmentType assessmentType = parseRow(row, rowNum);
                    if (assessmentType != null) {
                        assessmentTypes.add(assessmentType);
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error parsing row " + rowNum + ": " + e.getMessage());
                }
            }
        }
        return assessmentTypes;
    }

    private static AssessmentType parseRow(Row row, int rowNum) {
        // Validate Assessment Type Name (Column 1)
        if (row.getCell(1) == null || row.getCell(1).getStringCellValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Assessment type name is missing at row " + rowNum);
        }

        String name = row.getCell(1).getStringCellValue().trim();
        if (name.length() > 255) {
            throw new IllegalArgumentException("Assessment type name exceeds 255 characters at row " + rowNum);
        }

        AssessmentType assessmentType = new AssessmentType();
        assessmentType.setName(name);

        return assessmentType;
    }
}