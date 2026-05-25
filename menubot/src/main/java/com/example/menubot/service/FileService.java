package com.example.menubot.service;

import com.example.menubot.bot.MenuBot;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileService {

    @Value("${bot.token}")
    private String botToken;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final MenuBot bot;

    public FileService(@Lazy MenuBot bot) {
        this.bot = bot;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String saveTelegramPhoto(String fileId) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            File tgFile = bot.execute(getFile);
            String tgFilePath = tgFile.getFilePath();

            String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + tgFilePath;
            String fileName = UUID.randomUUID() + ".jpg";
            Path targetPath = Paths.get(uploadDir, fileName);

            try (InputStream in = new URL(downloadUrl).openStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}