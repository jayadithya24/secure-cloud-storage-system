package com.securecloud.storage.dto;

public class OwnerShareStatusResponse {

    private Long fileId;
    private String fileName;
    private String recipientEmail;
    private String permission;
    private boolean suspicious;

    public OwnerShareStatusResponse(Long fileId, String fileName, String recipientEmail, String permission, boolean suspicious) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.recipientEmail = recipientEmail;
        this.permission = permission;
        this.suspicious = suspicious;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isSuspicious() {
        return suspicious;
    }
}
