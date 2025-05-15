package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.example.hcm25_cpl_ks_java_01_lms.user.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class AttendanceImporter {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceImporter.class);

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final AttendanceService attendanceService;
    private final EmailAttendanceService emailAttendanceService;

    @Autowired
    public AttendanceImporter(AttendanceRepository attendanceRepository,
                              UserRepository userRepository,
                              AttendanceService attendanceService,
                              EmailAttendanceService emailAttendanceService) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.attendanceService = attendanceService;
        this.emailAttendanceService = emailAttendanceService;
    }

    /**
     * Tạo template Excel để import điểm danh
     */
    public ByteArrayInputStream generateImportTemplate(Long classId, LocalDate date) throws IOException {
        Classes classInfo = attendanceService.findClassById(classId);
        List<User> students = classInfo.getStudents();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(classInfo.getName() + "_" + date.toString());

            // Thiết lập styles
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Tạo header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Tên đăng nhập", "Họ và tên", "Email",
                    "Điểm danh", "Vắng có phép", "Giờ điểm danh", "Ghi chú"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Tạo một validation list cho Điểm danh (Có mặt, Vắng mặt)
            DataValidationHelper validationHelper = sheet.getDataValidationHelper();
            CellRangeAddressList statusAddressList = new CellRangeAddressList(1, students.size(), 4, 4);
            DataValidationConstraint statusConstraint = validationHelper.createExplicitListConstraint(
                    new String[]{"Có mặt", "Vắng mặt"});
            DataValidation statusValidation = validationHelper.createValidation(statusConstraint, statusAddressList);
            statusValidation.setShowErrorBox(true);
            sheet.addValidationData(statusValidation);

            // Tạo validation list cho Vắng có phép (Có, Không)
            CellRangeAddressList excusedAddressList = new CellRangeAddressList(1, students.size(), 5, 5);
            DataValidationConstraint excusedConstraint = validationHelper.createExplicitListConstraint(
                    new String[]{"Có", "Không"});
            DataValidation excusedValidation = validationHelper.createValidation(excusedConstraint, excusedAddressList);
            excusedValidation.setShowErrorBox(true);
            sheet.addValidationData(excusedValidation);

            // Fill dữ liệu
            CellStyle dataCellStyle = createDataStyle(workbook);
            // Tạo style thời gian
            CellStyle timeStyle = workbook.createCellStyle();
            timeStyle.cloneStyleFrom(dataCellStyle);
            CreationHelper createHelper = workbook.getCreationHelper();
            timeStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

            int rowIdx = 1;
            for (User student : students) {
                Row row = sheet.createRow(rowIdx++);

                // ID
                Cell idCell = row.createCell(0);
                idCell.setCellValue(student.getId());
                idCell.setCellStyle(dataCellStyle);

                // Tên đăng nhập
                Cell usernameCell = row.createCell(1);
                usernameCell.setCellValue(student.getUsername());
                usernameCell.setCellStyle(dataCellStyle);

                // Họ và tên
                Cell nameCell = row.createCell(2);
                String fullName = "";
                if (student.getLastName() != null) {
                    fullName += student.getLastName();
                }
                if (student.getFirstName() != null) {
                    if (!fullName.isEmpty()) fullName += " ";
                    fullName += student.getFirstName();
                }
                nameCell.setCellValue(fullName);
                nameCell.setCellStyle(dataCellStyle);

                // Email
                Cell emailCell = row.createCell(3);
                emailCell.setCellValue(student.getEmail());
                emailCell.setCellStyle(dataCellStyle);

                // Điểm danh - mặc định "Vắng mặt"
                Cell statusCell = row.createCell(4);
                statusCell.setCellValue("Vắng mặt");
                statusCell.setCellStyle(dataCellStyle);

                // Vắng có phép - mặc định "Không"
                Cell excusedCell = row.createCell(5);
                excusedCell.setCellValue("Không");
                excusedCell.setCellStyle(dataCellStyle);

                // Giờ điểm danh - mặc định để trống
                Cell timeCell = row.createCell(6);
                timeCell.setCellStyle(timeStyle);

                // Ghi chú - mặc định để trống
                Cell notesCell = row.createCell(7);
                notesCell.setCellValue("");
                notesCell.setCellStyle(dataCellStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Thêm sheet hướng dẫn
            Sheet instructionSheet = workbook.createSheet("Hướng dẫn");
            createInstructionSheet(workbook, instructionSheet);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    /**
     * Import điểm danh từ file Excel
     */
    public int importAttendanceFromExcel(Long classId, LocalDate date, InputStream inputStream) throws IOException {
        Classes classInfo = attendanceService.findClassById(classId);

        // Trước tiên, tạo bản ghi điểm danh cho lớp vào ngày này (nếu chưa có)
        attendanceService.createAttendanceForClass(classId, date);

        // Lấy danh sách attendance đã tạo
        List<Attendance> existingAttendance = attendanceRepository.findByClassInfoAndAttendanceDate(classInfo, date);

        // Tạo map theo ID và username để dễ tìm kiếm
        Map<Long, Attendance> attendanceByIdMap = new HashMap<>();
        Map<String, Attendance> attendanceByUsernameMap = new HashMap<>();

        for (Attendance a : existingAttendance) {
            if (a.getStudent() != null) {
                attendanceByIdMap.put(a.getStudent().getId(), a);
                attendanceByUsernameMap.put(a.getStudent().getUsername(), a);
            }
        }

        List<Attendance> updatedAttendances = new ArrayList<>();
        int recordsUpdated = 0;

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Bắt đầu từ dòng 1 (sau header)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Lấy ID sinh viên từ cột đầu tiên
                Cell idCell = row.getCell(0);
                if (idCell == null) continue;

                Long studentId = null;
                String username = null;
                Attendance attendance = null;

                // Thử lấy ID
                if (idCell.getCellType() == CellType.NUMERIC) {
                    studentId = (long) idCell.getNumericCellValue();
                    attendance = attendanceByIdMap.get(studentId);
                } else if (idCell.getCellType() == CellType.STRING) {
                    try {
                        studentId = Long.parseLong(idCell.getStringCellValue());
                        attendance = attendanceByIdMap.get(studentId);
                    } catch (NumberFormatException e) {
                        // Không phải số, bỏ qua
                    }
                }

                // Nếu không tìm thấy bằng ID, thử tìm bằng username
                if (attendance == null) {
                    Cell usernameCell = row.getCell(1);
                    if (usernameCell != null && usernameCell.getCellType() == CellType.STRING) {
                        username = usernameCell.getStringCellValue();
                        attendance = attendanceByUsernameMap.get(username);
                    }
                }

                // Nếu vẫn không tìm thấy, bỏ qua dòng này
                if (attendance == null) {
                    continue;
                }

                // Lưu trạng thái cũ để so sánh
                boolean wasPresent = attendance.getIsPresent() != null && attendance.getIsPresent();
                boolean wasExcused = attendance.getIsExcused() != null && attendance.getIsExcused();

                // Lấy trạng thái điểm danh (cột 4)
                Cell statusCell = row.getCell(4);
                boolean isPresent = false;
                if (statusCell != null) {
                    String status = statusCell.getStringCellValue();
                    isPresent = "Có mặt".equals(status);
                    attendance.setIsPresent(isPresent);
                }

                // Lấy trạng thái có phép (cột 5)
                Cell excusedCell = row.getCell(5);
                boolean isExcused = false;
                if (excusedCell != null) {
                    String excused = excusedCell.getStringCellValue();
                    isExcused = "Có".equals(excused);
                    attendance.setIsExcused(isExcused);
                }

                // Lấy giờ điểm danh (cột 6)
                Cell timeCell = row.getCell(6);
                if (timeCell != null) {
                    try {
                        if (timeCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(timeCell)) {
                            // Nếu là định dạng ngày tháng
                            Date cellDate = timeCell.getDateCellValue();
                            LocalDateTime checkinTime = cellDate.toInstant()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime();
                            attendance.setCheckedInTime(checkinTime);
                        } else if (timeCell.getCellType() == CellType.STRING && !timeCell.getStringCellValue().isEmpty()) {
                            // Nếu là string, thử parse theo định dạng
                            String timeStr = timeCell.getStringCellValue();
                            LocalDateTime checkinTime = LocalDateTime.parse(timeStr, timeFormatter);
                            attendance.setCheckedInTime(checkinTime);
                        }
                    } catch (Exception e) {
                        // Nếu không parse được, sử dụng thời gian hiện tại nếu sinh viên có mặt
                        if (isPresent) {
                            attendance.setCheckedInTime(LocalDateTime.now());
                        } else {
                            attendance.setCheckedInTime(null);
                        }
                    }
                } else if (isPresent) {
                    // Nếu không có giờ điểm danh nhưng sinh viên có mặt, sử dụng thời gian hiện tại
                    attendance.setCheckedInTime(LocalDateTime.now());
                } else {
                    attendance.setCheckedInTime(null);
                }

                // Lấy ghi chú (cột 7)
                Cell notesCell = row.getCell(7);
                if (notesCell != null) {
                    if (notesCell.getCellType() == CellType.STRING) {
                        attendance.setNotes(notesCell.getStringCellValue());
                    } else if (notesCell.getCellType() == CellType.NUMERIC) {
                        attendance.setNotes(String.valueOf(notesCell.getNumericCellValue()));
                    }
                }

                // Kiểm tra xem có thay đổi trạng thái không
                boolean statusChanged = (wasPresent != isPresent) || (wasExcused != isExcused);

                // Lưu attendance
                attendanceRepository.save(attendance);
                recordsUpdated++;

                // Thêm vào danh sách cần gửi email thông báo nếu có thay đổi trạng thái
                if (statusChanged) {
                    updatedAttendances.add(attendance);
                }
            }

            // Gửi email thông báo cập nhật trạng thái
            if (!updatedAttendances.isEmpty()) {
                emailAttendanceService.sendBulkAttendanceNotifications(updatedAttendances);
            }

            logger.info("Đã import {} bản ghi điểm danh cho lớp {} ngày {}",
                    recordsUpdated, classInfo.getName(), date);

            return recordsUpdated;
        }
    }

    /**
     * Tạo style cho header
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Tạo style cho cells dữ liệu
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
     * Tạo sheet hướng dẫn
     */
    private void createInstructionSheet(Workbook workbook, Sheet sheet) {
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Tạo tiêu đề
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Hướng dẫn sử dụng template điểm danh");
        titleCell.setCellStyle(titleStyle);

        int rowIndex = 2;

        // Hướng dẫn các cột
        Row statusHeaderRow = sheet.createRow(rowIndex++);
        Cell statusHeaderCell = statusHeaderRow.createCell(0);
        statusHeaderCell.setCellValue("Cột Điểm danh:");
        statusHeaderCell.setCellStyle(headerStyle);

        sheet.createRow(rowIndex++).createCell(0).setCellValue("- \"Có mặt\": Sinh viên có mặt trong buổi học");
        sheet.createRow(rowIndex++).createCell(0).setCellValue("- \"Vắng mặt\": Sinh viên vắng mặt");
        rowIndex++;

        // Vắng có phép
        Row excusedHeaderRow = sheet.createRow(rowIndex++);
        Cell excusedHeaderCell = excusedHeaderRow.createCell(0);
        excusedHeaderCell.setCellValue("Cột Vắng có phép:");
        excusedHeaderCell.setCellStyle(headerStyle);

        sheet.createRow(rowIndex++).createCell(0).setCellValue("- \"Có\": Sinh viên vắng có phép");
        sheet.createRow(rowIndex++).createCell(0).setCellValue("- \"Không\": Sinh viên vắng không phép");
        sheet.createRow(rowIndex++).createCell(0).setCellValue("- Chỉ áp dụng khi sinh viên vắng mặt");
        rowIndex++;

        // Giờ điểm danh
        Row timeHeaderRow = sheet.createRow(rowIndex++);
        Cell timeHeaderCell = timeHeaderRow.createCell(0);
        timeHeaderCell.setCellValue("Cột Giờ điểm danh:");
        timeHeaderCell.setCellStyle(headerStyle);

        sheet.createRow(rowIndex++).createCell(0).setCellValue("- Nhập thời gian điểm danh theo định dạng: yyyy-mm-dd hh:mm:ss");
        sheet.createRow(rowIndex++).createCell(0).setCellValue("- Ví dụ: 2025-04-25 08:30:00");
        sheet.createRow(rowIndex++).createCell(0).setCellValue("- Chỉ áp dụng khi sinh viên có mặt");
        sheet.createRow(rowIndex++).createCell(0).setCellValue("- Để trống nếu không có thông tin chính xác (hệ thống sẽ cập nhật thời gian hiện tại)");
        rowIndex++;

        // Ghi chú
        Row noteHeaderRow = sheet.createRow(rowIndex++);
        Cell noteHeaderCell = noteHeaderRow.createCell(0);
        noteHeaderCell.setCellValue("Cột Ghi chú:");
        noteHeaderCell.setCellStyle(headerStyle);

        sheet.createRow(rowIndex++).createCell(0).setCellValue("- Ghi chú về lý do vắng, đi trễ, hoặc thông tin khác");
        rowIndex++;

        // Lưu ý quan trọng
        Row importantHeaderRow = sheet.createRow(rowIndex++);
        Cell importantHeaderCell = importantHeaderRow.createCell(0);
        importantHeaderCell.setCellValue("LƯU Ý QUAN TRỌNG:");
        importantHeaderCell.setCellStyle(headerStyle);

        sheet.createRow(rowIndex++).createCell(0).setCellValue("1. KHÔNG thay đổi ID, Tên đăng nhập, Họ và tên, Email của sinh viên");
        sheet.createRow(rowIndex++).createCell(0).setCellValue("2. Chỉ sửa các cột: Điểm danh, Vắng có phép, Giờ điểm danh, Ghi chú");
        sheet.createRow(rowIndex++).createCell(0).setCellValue("3. Đảm bảo lưu file dưới định dạng .xlsx");

        // Auto-size columns
        sheet.autoSizeColumn(0);
    }
}