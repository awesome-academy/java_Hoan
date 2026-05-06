package com.fooddrinks.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    // Saves file to local storage, returns the public URL path (e.g. /uploads/filename.jpg)
    String store(MultipartFile file);

    void delete(String fileUrl);
}
