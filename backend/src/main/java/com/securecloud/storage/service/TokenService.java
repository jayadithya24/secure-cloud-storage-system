package com.securecloud.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.auth.secret:change-this-secret-in-production}")
    private String secret;

    @Value("${app.auth.ttl-ms:86400000}")
    private long ttlMs;

    public String generateToken(String email) {
        long expiry = System.currentTimeMillis() + ttlMs;
        String payload = email + ":" + expiry;
        String payloadPart = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signaturePart = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sign(payload));
        return payloadPart + "." + signaturePart;
    }

    public String validateAndExtractEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token format");
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        byte[] providedSignature = Base64.getUrlDecoder().decode(parts[1]);
        byte[] expectedSignature = sign(payload);

        if (!java.security.MessageDigest.isEqual(providedSignature, expectedSignature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token signature");
        }

        int delimiterIndex = payload.lastIndexOf(':');
        if (delimiterIndex <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token payload");
        }

        String email = payload.substring(0, delimiterIndex);
        long expiry;
        try {
            expiry = Long.parseLong(payload.substring(delimiterIndex + 1));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token expiry");
        }

        if (System.currentTimeMillis() > expiry) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
        }

        return email;
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }
}
