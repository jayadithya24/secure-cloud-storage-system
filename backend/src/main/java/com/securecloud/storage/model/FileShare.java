package com.securecloud.storage.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fileId;

    private String sharedWith;

    private String publicToken;   // ADD THIS

    private String permission;

    @Column(name = "max_downloads")
    private Integer maxDownloads;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "expiry_action")
    private String expiryAction;

    public Long getId() {
        return id;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(String sharedWith) {
        this.sharedWith = sharedWith;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public void setPublicToken(String publicToken) {
        this.publicToken = publicToken;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getExpiryAction() {
        return expiryAction;
    }

    public void setExpiryAction(String expiryAction) {
        this.expiryAction = expiryAction;
    }
}