package com.securecloud.storage.dto;

public class SharedFileResponse {

    private String fileName;
    private String ownerEmail;
    private String shareLink;
    private String permission;

    public SharedFileResponse(String fileName, String ownerEmail, String shareLink, String permission) {
        this.fileName = fileName;
        this.ownerEmail = ownerEmail;
        this.shareLink = shareLink;
        this.permission = permission;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getShareLink() {
        return shareLink;
    }

    public String getPermission() {
        return permission;
    }
}