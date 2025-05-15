package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
public class QRCodeService {
    private static final Logger logger = LoggerFactory.getLogger(QRCodeService.class);

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.qrcode.directory:qrcodes}")
    private String qrCodeDirectory;

    /**
     * Tạo URL điểm danh từ token - Tiếp tục hỗ trợ để tương thích ngược
     */
    public String generateAttendanceUrl(String token) {
        // Xử lý baseUrl để đảm bảo định dạng chuẩn
        String cleanBaseUrl = baseUrl.trim();

        // Đảm bảo baseUrl không kết thúc bằng dấu "/"
        if (cleanBaseUrl.endsWith("/")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
        }

        // Tạo URL điểm danh
        String attendanceUrl = cleanBaseUrl + "/attendance/qr/check?token=" + token;

        logger.info("Đã tạo URL điểm danh: {}", attendanceUrl);

        return attendanceUrl;
    }

    /**
     * Tạo URL trang submit từ token - Mới thêm
     */
    public String generateSubmitUrl(String token) {
        // Xử lý baseUrl để đảm bảo định dạng chuẩn
        String cleanBaseUrl = baseUrl.trim();

        // Đảm bảo baseUrl không kết thúc bằng dấu "/"
        if (cleanBaseUrl.endsWith("/")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
        }

        // Tạo URL submit
        String submitUrl = cleanBaseUrl + "/attendance/qr/submit?token=" + token;

        logger.info("Đã tạo URL submit: {}", submitUrl);

        return submitUrl;
    }

    /**
     * Tạo mã QR code từ URL dưới dạng Base64 String để hiển thị trực tiếp
     */
    public String generateQRCodeBase64(String attendanceUrl) throws WriterException, IOException {
        logger.info("Tạo mã QR code cho URL: {}", attendanceUrl);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(attendanceUrl, BarcodeFormat.QR_CODE, 250, 250);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        byte[] qrCodeBytes = outputStream.toByteArray();

        String base64Result = Base64.getEncoder().encodeToString(qrCodeBytes);
        logger.debug("Đã tạo mã QR code Base64 (độ dài: {})", base64Result.length());

        return base64Result;
    }

    /**
     * Tạo và lưu QR code dưới dạng file hình ảnh
     */
    public String generateQRCodeImage(String attendanceUrl) throws WriterException, IOException {
        logger.info("Tạo file hình ảnh QR code cho URL: {}", attendanceUrl);

        // Tạo thư mục nếu chưa tồn tại
        Path qrDir = Paths.get(qrCodeDirectory);
        if (!Files.exists(qrDir)) {
            Files.createDirectories(qrDir);
            logger.info("Đã tạo thư mục QR code: {}", qrDir.toAbsolutePath());
        }

        // Tạo tên file ngẫu nhiên
        String fileName = UUID.randomUUID().toString() + ".png";
        Path filePath = qrDir.resolve(fileName);

        // Tạo QR code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(attendanceUrl, BarcodeFormat.QR_CODE, 250, 250);

        // Lưu file
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", filePath);
        logger.info("Đã lưu QR code thành file: {}", filePath.toAbsolutePath());

        return fileName;
    }

    /**
     * Kiểm tra URL base đang được cấu hình
     */
    public String getConfiguredBaseUrl() {
        return this.baseUrl;
    }
}