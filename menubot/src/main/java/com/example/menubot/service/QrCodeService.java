package com.example.menubot.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class QrCodeService {

    private final Path uploadRoot;
    private final String botUsername;

    public QrCodeService(@Value("${app.upload.dir:./uploads}") String uploadDir,
                         @Value("${bot.username}") String botUsername) {
        this.uploadRoot = Path.of(uploadDir, "qr");
        this.botUsername = botUsername;
        try {
            Files.createDirectories(uploadRoot);
        } catch (Exception ignored) {}
    }

    public String buildDeepLink(long restaurantId, String tableNumber) {
        String startParam = restaurantId + "_table" + tableNumber;
        return "https://t.me/" + botUsername + "/app?startapp=" + startParam;
    }

    public String generateAndStorePng(long restaurantId, String tableNumber) {
        try {
            String link = buildDeepLink(restaurantId, tableNumber);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(link, BarcodeFormat.QR_CODE, 512, 512);
            String filename = "r" + restaurantId + "_t" + tableNumber + "_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            Path file = uploadRoot.resolve(filename);
            MatrixToImageWriter.writeToPath(matrix, "PNG", file);
            return "qr/" + filename;
        } catch (Exception e) {
            throw new IllegalStateException("QR generation failed", e);
        }
    }
}
