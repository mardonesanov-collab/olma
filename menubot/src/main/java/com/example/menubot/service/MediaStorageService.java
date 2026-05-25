package com.example.menubot.service;

import com.example.menubot.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    private final Path uploadRoot;

    public MediaStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir);
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload dir", e);
        }
    }

    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File too large (max 10MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WEBP allowed");
        }

        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        String filename = UUID.randomUUID() + ext;
        Path targetDir = uploadRoot.resolve(folder);
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return folder + "/" + filename;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed");
        }
    }

    public String toPublicUrl(String storedPath) {
        if (storedPath == null) return null;
        return "/uploads/" + storedPath;
    }
}
