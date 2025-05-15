package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.classes.ClassesRepository;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.example.hcm25_cpl_ks_java_01_lms.user.UserRepository;
import com.google.zxing.WriterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttendanceTokenService {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceTokenService.class);

    // Thời gian cho phép submit điểm danh (1 phút cuối trước khi hết hạn)
    private static final int SUBMIT_WINDOW_MINUTES = 1;

    private final AttendanceTokenRepository tokenRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final ClassesRepository classesRepository;
    private final UserRepository userRepository;
    private final QRCodeService qrCodeService;
    private final EmailAttendanceService emailAttendanceService;

    @Value("${app.token.expiration-minutes:2}")  // Thời gian hết hạn mặc định 5 phút (cho dễ kiểm tra)
    private int tokenExpirationMinutes;

    @Autowired
    public AttendanceTokenService(
            AttendanceTokenRepository tokenRepository,
            AttendanceRepository attendanceRepository,
            AttendanceService attendanceService,
            ClassesRepository classesRepository,
            UserRepository userRepository,
            QRCodeService qrCodeService,
            EmailAttendanceService emailAttendanceService) {
        this.tokenRepository = tokenRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
        this.classesRepository = classesRepository;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.emailAttendanceService = emailAttendanceService;
    }

    /**
     * Tạo token cho tất cả học viên trong một lớp học
     */
    @Transactional
    public List<AttendanceTokenDTO> generateTokensForClass(Long classId, LocalDate attendanceDate) {
        Classes classInfo = classesRepository.findById(classId)
                .orElseThrow(() -> new AttendanceException.ClassNotFoundException(classId));

        // Nếu chưa có danh sách điểm danh cho ngày này, tạo mới
        if (attendanceRepository.findByClassInfoAndAttendanceDate(classInfo, attendanceDate).isEmpty()) {
            attendanceService.createAttendanceForClass(classId, attendanceDate);
        }

        // Lấy danh sách học viên đã có bản ghi điểm danh
        List<Attendance> attendances = attendanceRepository.findByClassInfoAndAttendanceDate(classInfo, attendanceDate);
        List<AttendanceTokenDTO> tokenDTOs = new ArrayList<>();
        LocalDateTime creationTime = LocalDateTime.now();  // Thời điểm tạo token
        LocalDateTime expirationTime = creationTime.plusMinutes(tokenExpirationMinutes);

        // Đối với mỗi bản ghi điểm danh, tạo token tương ứng
        for (Attendance attendance : attendances) {
            // Bỏ qua nếu không có sinh viên
            if (attendance.getStudent() == null) {
                continue;
            }

            User student = attendance.getStudent();

            // Tạo token mới
            String tokenValue = UUID.randomUUID().toString();

            AttendanceToken token = AttendanceToken.builder()
                    .token(tokenValue)
                    .student(student)
                    .classInfo(classInfo)
                    .expiresAt(expirationTime)
                    .isUsed(false)
                    .createdAt(creationTime)  // Thêm thời điểm tạo token
                    .build();

            tokenRepository.save(token);

            // Tạo URL điểm danh và mã QR - sử dụng trang submit
            String attendanceUrl = qrCodeService.generateSubmitUrl(tokenValue);
            String qrCodeBase64 = "";

            try {
                qrCodeBase64 = "data:image/png;base64," + qrCodeService.generateQRCodeBase64(attendanceUrl);
            } catch (WriterException | IOException e) {
                logger.error("Error generating QR code: {}", e.getMessage(), e);
            }

            // Tạo DTO để trả về
            AttendanceTokenDTO dto = AttendanceTokenDTO.builder()
                    .token(tokenValue)
                    .studentId(student.getId())
                    .studentName(student.getUsername()) // hoặc getFullName() tùy thuộc vào User class của bạn
                    .studentEmail(student.getEmail())
                    .classId(classInfo.getId())
                    .className(classInfo.getName())
                    .expiresAt(expirationTime)
                    .createdAt(creationTime)  // Thêm thời gian tạo vào DTO
                    .isUsed(false)
                    .qrCodeImageUrl(qrCodeBase64)
                    .qrCodeUrl(attendanceUrl)
                    .build();

            tokenDTOs.add(dto);
        }

        return tokenDTOs;
    }

    /**
     * Xác thực token để hiển thị trang submit
     */
    @Transactional(readOnly = true)
    public AttendanceTokenDTO validateToken(String tokenValue) {
        logger.info("Đang xác thực token để hiển thị trang submit: {}", tokenValue);

        Optional<AttendanceToken> tokenOpt = tokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            logger.error("Token không tồn tại trong database: {}", tokenValue);
            throw new AttendanceException("Token không hợp lệ");
        }

        AttendanceToken token = tokenOpt.get();
        logger.info("Tìm thấy token cho sinh viên: {}, lớp: {}",
                token.getStudent().getUsername(), token.getClassInfo().getName());

        // Kiểm tra token đã dùng chưa
        if (token.getIsUsed()) {
            logger.error("Token đã được sử dụng trước đó vào lúc: {}", token.getUsedAt());
            throw new AttendanceException("Token đã được sử dụng");
        }

        // Kiểm tra token hết hạn chưa
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.error("Token đã hết hạn vào: {}, hiện tại là: {}",
                    token.getExpiresAt(), LocalDateTime.now());
            throw new AttendanceException("Token đã hết hạn");
        }

        // Tính thời điểm sớm nhất có thể submit (1 phút trước khi hết hạn)
        LocalDateTime earliestSubmitTime = token.getExpiresAt().minusMinutes(SUBMIT_WINDOW_MINUTES);

        // Trả về thông tin token cho trang submit, bao gồm thời điểm sớm nhất được phép submit
        return AttendanceTokenDTO.builder()
                .token(token.getToken())
                .studentId(token.getStudent().getId())
                .studentName(token.getStudent().getUsername()) // hoặc getFullName()
                .studentEmail(token.getStudent().getEmail())
                .classId(token.getClassInfo().getId())
                .className(token.getClassInfo().getName())
                .expiresAt(token.getExpiresAt())
                .createdAt(token.getCreatedAt())
                .isUsed(token.getIsUsed())
                .earliestSubmitTime(earliestSubmitTime) // Thêm trường này để frontend biết thời điểm có thể submit
                .build();
    }

    /**
     * Xác thực token và điểm danh sau khi submit
     */
    @Transactional
    public AttendanceResultDTO checkAttendance(String tokenValue) {
        logger.info("Bắt đầu kiểm tra token: {}", tokenValue);

        Optional<AttendanceToken> tokenOpt = tokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            logger.error("Token không tồn tại trong database: {}", tokenValue);
            throw new AttendanceException("Token không hợp lệ");
        }

        AttendanceToken token = tokenOpt.get();
        logger.info("Tìm thấy token cho sinh viên: {}, lớp: {}",
                token.getStudent().getUsername(), token.getClassInfo().getName());

        // Kiểm tra token đã dùng chưa
        if (token.getIsUsed()) {
            logger.error("Token đã được sử dụng trước đó vào lúc: {}", token.getUsedAt());
            throw new AttendanceException("Token đã được sử dụng");
        }

        // Kiểm tra token hết hạn chưa
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.error("Token đã hết hạn vào: {}, hiện tại là: {}",
                    token.getExpiresAt(), LocalDateTime.now());
            throw new AttendanceException("Token đã hết hạn");
        }

        // Tính thời điểm sớm nhất có thể submit (1 phút cuối trước khi hết hạn)
        LocalDateTime earliestSubmitTime = token.getExpiresAt().minusMinutes(SUBMIT_WINDOW_MINUTES);

        // Kiểm tra xem đã đến thời gian cho phép submit chưa
        if (LocalDateTime.now().isBefore(earliestSubmitTime)) {
            long secondsToWait = ChronoUnit.SECONDS.between(LocalDateTime.now(), earliestSubmitTime);
            logger.error("Chưa đến thời gian điểm danh, cần đợi thêm {} giây", secondsToWait);
            throw new AttendanceException("Vui lòng đợi thêm " + secondsToWait + " giây nữa để điểm danh");
        }

        // Đánh dấu token đã dùng
        token.setIsUsed(true);
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
        logger.info("Đã đánh dấu token đã sử dụng");

        // Tìm bản ghi điểm danh tương ứng
        Optional<Attendance> attendanceOpt = attendanceRepository.findByClassInfoAndStudentAndAttendanceDate(
                token.getClassInfo(),
                token.getStudent(),
                LocalDate.now());

        if (attendanceOpt.isEmpty()) {
            logger.error("Không tìm thấy bản ghi điểm danh cho sinh viên: {}, lớp: {}, ngày: {}",
                    token.getStudent().getUsername(), token.getClassInfo().getName(), LocalDate.now());
            throw new AttendanceException("Không tìm thấy bản ghi điểm danh cho hôm nay");
        }

        Attendance attendance = attendanceOpt.get();
        attendance.setIsPresent(true);
        attendance.setCheckedInTime(LocalDateTime.now());
        attendanceRepository.save(attendance);
        logger.info("Đã cập nhật bản ghi điểm danh: isPresent=true, checkedInTime={}",
                LocalDateTime.now());

        // Gửi email thông báo điểm danh thành công
        try {
            // Sử dụng service sẵn có để gửi email thông báo
            emailAttendanceService.sendAttendanceStatusNotification(attendance);
            logger.info("Đã gửi email thông báo điểm danh thành công cho: {}", token.getStudent().getEmail());
        } catch (Exception e) {
            // Chỉ log lỗi, không ảnh hưởng đến quá trình điểm danh
            logger.error("Không thể gửi email thông báo điểm danh: {}", e.getMessage(), e);
        }

        // Trả về kết quả
        AttendanceResultDTO result = new AttendanceResultDTO(
                token.getStudent().getStudentCode(),
                token.getStudent().getFullName(),
                LocalDateTime.now()
        );
        logger.info("Hoàn tất xử lý điểm danh thành công cho sinh viên: {}", result.getStudentName());

        return result;
    }

    /**
     * Xóa tất cả các token hết hạn
     */
    @Transactional
    public int cleanupExpiredTokens() {
        List<AttendanceToken> expiredTokens = tokenRepository.findAll().stream()
                .filter(token -> token.getExpiresAt().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList());

        if (!expiredTokens.isEmpty()) {
            tokenRepository.deleteAll(expiredTokens);
        }

        return expiredTokens.size();
    }
}