package com.example.hcm25_cpl_ks_java_01_lms.assessment;

import com.example.hcm25_cpl_ks_java_01_lms.assessmentType.AssessmentType;
import com.example.hcm25_cpl_ks_java_01_lms.course.Course;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AssessmentExcelImporter {
    public static List<Assessment> importAssessments(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        List<Assessment> assessments = new ArrayList<>();
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
                    Assessment assessment = parseRow(row, rowNum);
                    if (assessment != null) {
                        assessments.add(assessment);
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error parsing row " + rowNum + ": " + e.getMessage());
                }
            }
        }
        return assessments;
    }

    private static Assessment parseRow(Row row, int rowNum) {
        // Validate Assessment Title (Column 2)
        if (row.getCell(2) == null || row.getCell(2).getStringCellValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Assessment title is missing at row " + rowNum);
        }

        String title = row.getCell(2).getStringCellValue().trim();
        if (title.length() > 255) {
            throw new IllegalArgumentException("Assessment title exceeds 255 characters at row " + rowNum);
        }

        Assessment assessment = new Assessment();
        assessment.setTitle(title);
        assessment.setTotalScore(null); // Đặt null theo yêu cầu
        assessment.setMinimumScore(null); // Đặt null theo yêu cầu
        assessment.setTimeLimit(null); // Đặt null theo yêu cầu

        // Parse AssessmentType từ cột 3 (giả sử tên AssessmentType nằm ở cột 3)
        if (row.getCell(3) != null && !row.getCell(3).getStringCellValue().trim().isEmpty()) {
            String assessmentTypeName = row.getCell(3).getStringCellValue().trim();
            if (!assessmentTypeName.equals("N/A")) {
                AssessmentType assessmentType = new AssessmentType();
                assessmentType.setName(assessmentTypeName); // Giả sử AssessmentType có trường name
                assessment.setAssessmentType(assessmentType);
            }
        }

        // Parse Course từ cột 1 (giả sử tên Course nằm ở cột 1)
        if (row.getCell(1) != null && !row.getCell(1).getStringCellValue().trim().isEmpty()) {
            String courseName = row.getCell(1).getStringCellValue().trim();
            if (!courseName.equals("N/A")) {
                Course course = new Course();
                course.setName(courseName); // Giả sử Course có trường name
                assessment.setCourse(course);
            }
        }

        // Exercises và Questions để rỗng theo yêu cầu, không cần parse

        return assessment;
    }
}