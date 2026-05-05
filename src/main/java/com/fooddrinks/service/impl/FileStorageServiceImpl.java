package com.fooddrinks.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fooddrinks.exception.BadRequestException;
import com.fooddrinks.service.FileStorageService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    // Map MIME type → extension. Dùng để derive extension an toàn từ MIME type
    // (không tin client-supplied filename)
    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    // Đọc giá trị app.upload.dir từ application.yml (hiện tại là "uploads/")
    @Value("${app.upload.dir}")
    private String uploadDir;

    // Path tuyệt đối đến thư mục lưu ảnh — được set 1 lần lúc khởi động
    private Path uploadPath;

    /**
     * Phương thức này được đánh dấu @PostConstruct để Spring tự động gọi sau khi
     * khởi tạo bean.
     * Mục đích: chuyển uploadDir (String) thành Path tuyệt đối và
     * tạo thư mục nếu chưa có.
     * Nếu không làm bước này, Files.copy() bên dưới sẽ báo lỗi "No
     * such file or directory".
     */
    @PostConstruct
    public void init() {
        // toAbsolutePath(): "uploads/" → "/Users/xxx/Projects/java_Hoan/uploads"
        // normalize(): loại bỏ các ký tự thừa như "..", "."
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            // Tạo thư mục nếu chưa tồn tại (createDirectories tạo cả thư mục cha)
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadPath, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        // Validate file trước khi lưu
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        // Extension từ MIME type — không tin client-supplied filename
        String extension = MIME_TO_EXTENSION.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Only JPEG, PNG and WebP images are allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("File size must not exceed 5MB");
        }

        // Đọc toàn bộ bytes 1 lần — dùng cho cả validation lẫn ghi disk
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }

        // Validate actual image bytes bằng ImageIO — phòng MIME spoofing:
        // client có thể gửi Content-Type: image/jpeg nhưng bytes thực là script/binary.
        // ImageIO.read() trả null nếu bytes không decode được thành ảnh hợp lệ.
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                throw new BadRequestException("File content is not a valid image");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("File content is not a valid image");
        }

        String filename = UUID.randomUUID() + extension;
        Path targetPath = uploadPath.resolve(filename).normalize();

        if (!targetPath.startsWith(uploadPath)) {
            throw new BadRequestException("Invalid file path");
        }

        try {
            Files.write(targetPath, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }

        // Trả về URL public để client có thể truy cập ảnh qua browser
        // Ví dụ: /uploads/a3f2b1c4-....jpg
        // WebConfig đã map /uploads/** → thư mục uploads/ trên disk
        return "/uploads/" + filename;
    }

    @Override
    public void delete(String fileUrl) {
        // Chỉ xử lý URL đúng format /uploads/... — bỏ qua input không hợp lệ
        if (fileUrl == null || !fileUrl.startsWith("/uploads/"))
            return;

        // Lấy tên file từ URL: "/uploads/abc.jpg" → "abc.jpg"
        String filename = fileUrl.substring("/uploads/".length());
        Path filePath = uploadPath.resolve(filename).normalize();

        // Kiểm tra path traversal: đảm bảo filePath nằm trong uploadPath
        // Ví dụ: filename = "../secret.txt" → filePath nằm ngoài uploadPath → bỏ qua
        if (!filePath.startsWith(uploadPath)) {
            log.warn("Rejected suspicious delete path: {}", filePath);
            return;
        }

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Không throw exception — xóa file thất bại không nên làm hỏng luồng nghiệp vụ
            // Nhưng log lại để dễ debug (permission, disk error, wrong path...)
            log.warn("Failed to delete file [{}]: {}", filePath, e.getMessage());
        }
    }
}
