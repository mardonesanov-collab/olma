package com.example.menubot.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TelegramInitDataValidator {

    private final String botToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramInitDataValidator(@Value("${bot.token}") String botToken) {
        this.botToken = botToken;
    }

    public Optional<TelegramUserPayload> validate(String initData) {
        if (initData == null || initData.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, String> params = parseQuery(initData);
            String receivedHash = params.remove("hash");
            if (receivedHash == null) return Optional.empty();

            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));

            byte[] secretKey = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8), botToken.getBytes(StandardCharsets.UTF_8));
            String calculated = bytesToHex(hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

            if (!MessageDigest.isEqual(calculated.getBytes(StandardCharsets.UTF_8), receivedHash.getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            String userJson = params.get("user");
            if (userJson == null) return Optional.empty();

            JsonNode user = objectMapper.readTree(userJson);
            long id = user.get("id").asLong();
            String username = user.hasNonNull("username") ? user.get("username").asText() : null;
            String firstName = user.hasNonNull("first_name") ? user.get("first_name").asText() : "";
            String lastName = user.hasNonNull("last_name") ? user.get("last_name").asText() : "";

            return Optional.of(new TelegramUserPayload(id, username, firstName, lastName));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Map<String, String> parseQuery(String initData) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String part : initData.split("&")) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }

    private byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public record TelegramUserPayload(long tgId, String username, String firstName, String lastName) {}
}
