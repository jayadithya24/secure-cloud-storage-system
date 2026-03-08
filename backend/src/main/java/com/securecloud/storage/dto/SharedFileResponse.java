package com.securecloud.storage.dto;

public class SharedFileResponse {

    private String fileName;
    private String ownerEmail;

    public SharedFileResponse(String fileName, String ownerEmail) {
        this.fileName = fileName;
        this.ownerEmail = ownerEmail;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }
}