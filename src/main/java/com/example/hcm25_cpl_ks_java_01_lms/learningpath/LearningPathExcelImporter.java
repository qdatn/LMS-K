package com.example.hcm25_cpl_ks_java_01_lms.learningpath;

import com.example.hcm25_cpl_ks_java_01_lms.course.Course;
import com.example.hcm25_cpl_ks_java_01_lms.course.CourseRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


public class LearningPathExcelImporter {

    public static List<LearningPath> importLearningPaths(InputStream inputStream, CourseRepository courseRepository) throws IOException {
        List<LearningPath> learningPaths = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);

        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();

        int rowNumber = 0;
        while (rows.hasNext()) {
            Row currentRow = rows.next();
            if (rowNumber == 0) {
                rowNumber++;
                continue;
            }

            Iterator<Cell> cellsInRow = currentRow.iterator();

            LearningPath learningPath = new LearningPath();
            List<Course> courses = new ArrayList<>();
            int cellIdx = 0;
            while (cellsInRow.hasNext()) {
                Cell currentCell = cellsInRow.next();
                switch (cellIdx) {
                    case 0:
                        // Bỏ qua cột id
                        break;
                    case 1:
                        learningPath.setName(currentCell.getStringCellValue().trim());
                        break;
                    case 2:
                        learningPath.setDescription(currentCell.getStringCellValue().trim());
                        break;
                    case 3:
                        String courseIds = "";
                        if (currentCell.getCellType() == CellType.NUMERIC) {
                            courseIds = String.valueOf((long) currentCell.getNumericCellValue());
                        } else if (currentCell.getCellType() == CellType.STRING) {
                            courseIds = currentCell.getStringCellValue();
                        }
                        if (courseIds != null && !courseIds.isEmpty()) {
                            courses = java.util.Arrays.stream(courseIds.split(","))
                                    .map(String::trim)
                                    .map(Long::parseLong)
                                    .map(courseRepository::findById)
                                    .filter(java.util.Optional::isPresent)
                                    .map(java.util.Optional::get)
                                    .collect(Collectors.toList());
                        }
                        learningPath.setCourses(courses);
                        break;
                    default:
                        break;
                }
                cellIdx++;
            }
            learningPaths.add(learningPath);
        }

        workbook.close();
        return learningPaths;
    }
}

