package com.securecloud.storage.dto;

import java.time.Instant;

public class SuspiciousUserResponse {

    private Long userId;
    private String email;
    private boolean suspicious;
    private String reason;
    private Instant detectedAt;

    public SuspiciousUserResponse(Long userId, String email, boolean suspicious, String reason, Instant detectedAt) {
        this.userId = userId;
        this.email = email;
        this.suspicious = suspicious;
        this.reason = reason;
        this.detectedAt = detectedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isSuspicious() {
        return suspicious;
    }

    public String getReason() {
        return reason;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }
}
