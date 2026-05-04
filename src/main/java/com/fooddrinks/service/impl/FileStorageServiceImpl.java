package com.fooddrinks.service.impl;

import com.fooddrinks.exception.BadRequestException;
import com.fooddrinks.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    // Chỉ cho phép upload 3 định dạng ảnh này (kiểm tra qua MIME type)
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    // Đọc giá trị app.upload.dir từ application.yml (hiện tại là "uploads/")
    @Value("${app.upload.dir}")
    private String uploadDir;

    // Path tuyệt đối đến thư mục lưu ảnh — được set 1 lần lúc khởi động
    private Path uploadPath;

    /**
     * @PostConstruct: chạy tự động 1 lần ngay sau khi Spring khởi tạo bean này.
     * Mục đích: chuyển uploadDir (String) thành Path tuyệt đối và tạo thư mục nếu chưa có.
     * Nếu không làm bước này, Files.copy() bên dưới sẽ báo lỗi "No such file or directory".
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
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only JPEG, PNG and WebP images are allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("File size must not exceed 5MB");
        }

        // Lấy đuôi file (.jpg, .png...) từ tên file gốc
        String extension = getExtension(file.getOriginalFilename());

        // Đặt tên file ngẫu nhiên bằng UUID để tránh trùng tên và ẩn tên gốc
        // Ví dụ: "a3f2b1c4-...-d5e6.jpg"
        String filename = UUID.randomUUID() + extension;

        // resolve(): ghép uploadPath + filename thành đường dẫn đầy đủ
        // Ví dụ: /Users/xxx/uploads/a3f2b1c4-....jpg
        Path targetPath = uploadPath.resolve(filename);

        try {
            // Đọc dữ liệu từ file upload và ghi vào targetPath trên disk
            Files.copy(file.getInputStream(), targetPath);
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
        if (fileUrl == null || fileUrl.isBlank()) return;
        // Lấy tên file từ URL: "/uploads/abc.jpg" → "abc.jpg"
        String filename = fileUrl.replace("/uploads/", "");
        // normalize() ngăn path traversal attack (ví dụ filename = "../../secret.txt")
        Path filePath = uploadPath.resolve(filename).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Không throw exception — việc xóa file thất bại không nên làm hỏng luồng nghiệp vụ
        }
    }

    // Lấy phần đuôi mở rộng từ tên file, mặc định là ".jpg" nếu không tìm thấy
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
