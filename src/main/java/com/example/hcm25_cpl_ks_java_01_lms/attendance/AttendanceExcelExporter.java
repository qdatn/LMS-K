package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AttendanceExcelExporter {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceExcelExporter.class);

    public ByteArrayInputStream exportToExcel(List<Attendance> attendanceList, LocalDate date, String className) throws IOException {
        logger.info("Starting Excel export for class: {}, date: {}, records: {}",
                className, date, attendanceList != null ? attendanceList.size() : 0);

        // Validate input parameters
        if (attendanceList == null || attendanceList.isEmpty()) {
            logger.error("No attendance records found for export");
            throw new IllegalArgumentException("No attendance records to export");
        }

        if (date == null) {
            logger.error("Date cannot be null for export");
            throw new IllegalArgumentException("Date cannot be null");
        }

        if (className == null || className.trim().isEmpty()) {
            className = "Class"; // Default name if none provided
            logger.warn("No class name provided, using default: {}", className);
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Create a valid Excel sheet name (max 31 chars, no special chars)
            String rawSheetName = className + "_" + date.toString();
            String sheetName = sanitizeSheetName(rawSheetName);
            logger.debug("Created sheet name: {} (from original: {})", sheetName, rawSheetName);

            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);

            // Create header cell style
            CellStyle headerCellStyle = createHeaderStyle(workbook);

            // Add headers
            String[] headers = {"ID", "Student Name", "Email", "Status", "Excused", "Check-in Time", "Notes"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
                sheet.autoSizeColumn(i);
            }

            // Create data styles
            CellStyle dataCellStyle = createDataStyle(workbook);
            CellStyle presentCellStyle = createPresentStyle(workbook, dataCellStyle);
            CellStyle absentCellStyle = createAbsentStyle(workbook, dataCellStyle);
            CellStyle excusedCellStyle = createExcusedStyle(workbook, dataCellStyle);
            CellStyle yesStyle = createYesStyle(workbook, dataCellStyle);
            CellStyle noStyle = createNoStyle(workbook, dataCellStyle);

            // Format for date/time
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Populate data rows
            int rowIdx = 1;
            for (Attendance attendance : attendanceList) {
                if (attendance == null) {
                    logger.warn("Skipping null attendance record at index: {}", rowIdx);
                    continue;
                }

                try {
                    Row row = sheet.createRow(rowIdx++);

                    // ID column
                    Cell idCell = row.createCell(0);
                    idCell.setCellValue(attendance.getId() != null ? attendance.getId() : 0);
                    idCell.setCellStyle(dataCellStyle);

                    // Student Name column
                    Cell nameCell = row.createCell(1);
                    String fullName = getStudentFullName(attendance);
                    nameCell.setCellValue(fullName);
                    nameCell.setCellStyle(dataCellStyle);

                    // Email column
                    Cell emailCell = row.createCell(2);
                    String email = getStudentEmail(attendance);
                    emailCell.setCellValue(email);
                    emailCell.setCellStyle(dataCellStyle);

                    // Status column
                    Cell statusCell = row.createCell(3);
                    String status;
                    CellStyle statusStyle;

                    if (Boolean.TRUE.equals(attendance.getIsPresent())) {
                        status = "Có mặt";
                        statusStyle = presentCellStyle;
                    } else {
                        if (Boolean.TRUE.equals(attendance.getIsExcused())) {
                            status = "Vắng có phép";
                            statusStyle = excusedCellStyle;
                        } else {
                            status = "Vắng không phép";
                            statusStyle = absentCellStyle;
                        }
                    }

                    statusCell.setCellValue(status);
                    statusCell.setCellStyle(statusStyle);

                    // Excused column
                    Cell excusedCell = row.createCell(4);
                    Boolean isExcused = attendance.getIsExcused();
                    String excusedText = (Boolean.TRUE.equals(isExcused)) ? "Có" : "Không";
                    excusedCell.setCellValue(excusedText);
                    excusedCell.setCellStyle(Boolean.TRUE.equals(isExcused) ? yesStyle : noStyle);

                    // Check-in Time column
                    Cell timeCell = row.createCell(5);
                    if (attendance.getCheckedInTime() != null) {
                        try {
                            timeCell.setCellValue(attendance.getCheckedInTime().format(dateTimeFormatter));
                        } catch (Exception e) {
                            logger.warn("Error formatting check-in time: {}", e.getMessage());
                            timeCell.setCellValue("Invalid date");
                        }
                    } else {
                        timeCell.setCellValue("N/A");
                    }
                    timeCell.setCellStyle(dataCellStyle);

                    // Notes column
                    Cell notesCell = row.createCell(6);
                    notesCell.setCellValue(attendance.getNotes() != null ? attendance.getNotes() : "");
                    notesCell.setCellStyle(dataCellStyle);

                } catch (Exception e) {
                    logger.error("Error processing attendance record at index {}: {}", rowIdx - 1, e.getMessage());
                    // Continue processing other records instead of failing completely
                }
            }

            // Add summary section
            addSummarySection(workbook, sheet, attendanceList, rowIdx);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to output stream
            workbook.write(out);
            logger.info("Excel export completed successfully with {} records", rowIdx - 1);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            logger.error("Failed to export attendance data to Excel: {}", e.getMessage(), e);
            throw new IOException("Failed to export attendance data: " + e.getMessage(), e);
        }
    }

    /**
     * Sanitizes the sheet name to be valid for Excel
     * - Max 31 characters
     * - No invalid characters: \ / ? * [ ] :
     */
    private String sanitizeSheetName(String name) {
        if (name == null || name.isEmpty()) {
            return "Sheet1";
        }

        // Replace invalid characters
        String sanitized = name.replaceAll("[\\\\/:*?\\[\\]]", "_");

        // Limit to 31 characters (Excel's limit)
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }

        return sanitized;
    }

    /**
     * Gets full name of student with proper null checks
     */
    private String getStudentFullName(Attendance attendance) {
        String fullName = "";

        if (attendance == null || attendance.getStudent() == null) {
            return "Unknown";
        }

        if (attendance.getStudent().getLastName() != null) {
            fullName += attendance.getStudent().getLastName().trim();
        }

        if (attendance.getStudent().getFirstName() != null) {
            if (!fullName.isEmpty()) fullName += " ";
            fullName += attendance.getStudent().getFirstName().trim();
        }

        return fullName.isEmpty() ? "No Name" : fullName;
    }

    /**
     * Gets student email with proper null checks
     */
    private String getStudentEmail(Attendance attendance) {
        if (attendance == null || attendance.getStudent() == null) {
            return "";
        }

        return attendance.getStudent().getEmail() != null
                ? attendance.getStudent().getEmail()
                : "";
    }

    /**
     * Creates header cell style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        style.setFont(headerFont);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Creates basic data cell style
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Creates style for "present" status
     */
    private CellStyle createPresentStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Creates style for "absent" status
     */
    private CellStyle createAbsentStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Creates style for "excused" status
     */
    private CellStyle createExcusedStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Creates style for "yes" values
     */
    private CellStyle createYesStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Creates style for "no" values
     */
    private CellStyle createNoStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Adds summary section to the sheet
     */
    private void addSummarySection(Workbook workbook, Sheet sheet, List<Attendance> attendanceList, int startRowIdx) {
        try {
            int summaryRowIdx = startRowIdx + 2;
            Row summaryRow = sheet.createRow(summaryRowIdx);
            Cell summaryLabelCell = summaryRow.createCell(0);
            summaryLabelCell.setCellValue("Thống kê điểm danh:");

            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            CellStyle summaryStyle = workbook.createCellStyle();
            summaryStyle.setFont(summaryFont);
            summaryLabelCell.setCellStyle(summaryStyle);

            int totalStudents = attendanceList.size();

            // Safe count using streams with null checks
            long presentCount = attendanceList.stream()
                    .filter(a -> a != null && Boolean.TRUE.equals(a.getIsPresent()))
                    .count();

            long absentCount = attendanceList.stream()
                    .filter(a -> a != null &&
                            (!Boolean.TRUE.equals(a.getIsPresent()) &&
                                    !Boolean.TRUE.equals(a.getIsExcused())))
                    .count();

            long excusedCount = attendanceList.stream()
                    .filter(a -> a != null &&
                            (!Boolean.TRUE.equals(a.getIsPresent()) &&
                                    Boolean.TRUE.equals(a.getIsExcused())))
                    .count();

            double presentRate = totalStudents > 0 ? (double) presentCount / totalStudents * 100 : 0;
            double absentRate = totalStudents > 0 ? (double) absentCount / totalStudents * 100 : 0;
            double excusedRate = totalStudents > 0 ? (double) excusedCount / totalStudents * 100 : 0;

            // Create stats rows
            Row statsRow1 = sheet.createRow(summaryRowIdx + 1);
            statsRow1.createCell(0).setCellValue("Tổng số học viên:");
            statsRow1.createCell(1).setCellValue(totalStudents);

            Row statsRow2 = sheet.createRow(summaryRowIdx + 2);
            statsRow2.createCell(0).setCellValue("Số học viên có mặt:");
            statsRow2.createCell(1).setCellValue(presentCount);
            statsRow2.createCell(2).setCellValue(String.format("%.2f%%", presentRate));

            Row statsRow3 = sheet.createRow(summaryRowIdx + 3);
            statsRow3.createCell(0).setCellValue("Số học viên vắng không phép:");
            statsRow3.createCell(1).setCellValue(absentCount);
            statsRow3.createCell(2).setCellValue(String.format("%.2f%%", absentRate));

            Row statsRow4 = sheet.createRow(summaryRowIdx + 4);
            statsRow4.createCell(0).setCellValue("Số học viên vắng có phép:");
            statsRow4.createCell(1).setCellValue(excusedCount);
            statsRow4.createCell(2).setCellValue(String.format("%.2f%%", excusedRate));
        } catch (Exception e) {
            logger.warn("Error creating summary section: {}", e.getMessage());
            // Continue without summary if there's an error
        }
    }
}