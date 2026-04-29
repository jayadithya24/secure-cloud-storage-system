package com.securecloud.storage.controller;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.securecloud.storage.model.FileEntity;
import com.securecloud.storage.model.FileShare;
import com.securecloud.storage.model.User;
import com.securecloud.storage.dto.OwnerShareStatusResponse;
import com.securecloud.storage.dto.SharedFileResponse;
import com.securecloud.storage.repository.FileShareRepository;
import com.securecloud.storage.repository.FileRepository;
import com.securecloud.storage.repository.UserRepository;
import com.securecloud.storage.service.FileService;
import com.securecloud.storage.service.EmailService;
import com.securecloud.storage.service.AnomalyDetectionService;
import com.securecloud.storage.service.ActivityLogService;
import com.securecloud.storage.util.FileEncryptionUtil;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileShareRepository fileShareRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @Autowired
    private ActivityLogService activityLogService;

    // Upload file
    @PostMapping("/upload")
    public FileEntity upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("email") String email
    ) throws Exception {

        return fileService.uploadFile(file, email);
    }

    // List user files
    @GetMapping("/files")
    public List<FileEntity> getFiles(@RequestParam String email) {
        return fileService.getFilesByUser(email);
    }

    // Download file (WITH DECRYPTION)
    @GetMapping("/download/{id}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long id, HttpServletRequest request) throws Exception {

        FileEntity fileEntity = fileService.getFileById(id);

        if (fileEntity == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(fileEntity.getFilePath());

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        User owner = userRepository.findByEmail(fileEntity.getOwnerEmail());
        anomalyDetectionService.validateAndTrackDownload(owner, request);

        // Read encrypted file
        byte[] encryptedData = Files.readAllBytes(path);

        // Decrypt file
        byte[] decryptedData = FileEncryptionUtil.decrypt(encryptedData);

        ByteArrayResource resource = new ByteArrayResource(decryptedData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileEntity.getFileName() + "\"")
                .contentLength(decryptedData.length)
                .body(resource);
    }

    // Share file
    @PostMapping("/share")
    public ResponseEntity<String> shareFile(
        @RequestParam Long fileId,
        @RequestParam String email,
        @RequestParam(defaultValue = "VIEW") String permission,
        @RequestParam(required = false) Integer maxDownloads,
        @RequestParam(required = false) String expiresAt
) {
    String normalizedEmail = email == null ? "" : email.trim();
    if (normalizedEmail.isBlank()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Recipient email is required");
    }

    String normalizedPermission = permission == null ? "VIEW" : permission.trim().toUpperCase();
    if (!"VIEW".equals(normalizedPermission) && !"EDIT".equals(normalizedPermission)) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Permission must be VIEW or EDIT");
    }

    if (maxDownloads != null && maxDownloads < 1) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("maxDownloads must be at least 1");
    }

    Instant expiresAtInstant = null;
    if (expiresAt != null && !expiresAt.isBlank()) {
        try {
            expiresAtInstant = Instant.parse(expiresAt.trim());
        } catch (DateTimeParseException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("expiresAt must be a valid ISO-8601 datetime (for example 2026-04-16T12:00:00Z)");
        }

        if (!expiresAtInstant.isAfter(Instant.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("expiresAt must be in the future");
        }
    }

        FileEntity file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("File not found");
        }

    FileShare share = fileShareRepository.findByFileIdAndSharedWith(fileId, normalizedEmail);
    boolean alreadyShared = share != null;

    if (!alreadyShared) {
        String token = UUID.randomUUID().toString();
        share = new FileShare();
        share.setFileId(fileId);
        share.setSharedWith(normalizedEmail);
        share.setPublicToken(token);
    }

    share.setPermission(normalizedPermission);
    share.setMaxDownloads(maxDownloads);
    share.setExpiresAt(expiresAtInstant);
    share.setExpiryAction("BLOCK");
    if (share.getDownloadCount() == null) {
        share.setDownloadCount(0);
    }
    fileShareRepository.save(share);

    String shareLink = "http://localhost:8080/api/public/download/" + share.getPublicToken();
    boolean emailSent = emailService.sendShareLinkEmail(
            normalizedEmail,
            file.getFileName(),
            file.getOwnerEmail(),
            shareLink
    );

    if (emailSent) {
        return ResponseEntity.ok("Share created successfully.");
    }

    return ResponseEntity.ok("Share saved. Email delivery is not configured in this environment. Share link: "
            );
}

    // View shared files
    @GetMapping("/shared-files")
    public List<SharedFileResponse> getSharedFiles(@RequestParam String email) {

        List<FileShare> shares = fileShareRepository.findBySharedWith(email);

        List<SharedFileResponse> result = new ArrayList<>();

        for (FileShare share : shares) {

            FileEntity file = fileRepository.findById(share.getFileId()).orElse(null);

            if (file != null) {
                String shareLink = "http://localhost:8080/api/public/download/" + share.getPublicToken();
                String permission = share.getPermission() == null ? "VIEW" : share.getPermission();
                result.add(new SharedFileResponse(
                        file.getFileName(),
                        file.getOwnerEmail(),
                        shareLink,
                        permission
                ));
            }
        }

        return result;
    }

    @GetMapping("/owner-share-status")
    public List<OwnerShareStatusResponse> getOwnerShareStatus(@RequestParam String email) {

        List<FileEntity> ownerFiles = fileService.getFilesByUser(email);
        List<OwnerShareStatusResponse> result = new ArrayList<>();

        for (FileEntity file : ownerFiles) {
            List<FileShare> shares = fileShareRepository.findByFileId(file.getId());

            for (FileShare share : shares) {
                User recipient = userRepository.findByEmail(share.getSharedWith());
                boolean isSuspicious = recipient != null && recipient.isSuspicious();
                String permission = share.getPermission() == null ? "VIEW" : share.getPermission();

                result.add(new OwnerShareStatusResponse(
                        file.getId(),
                        file.getFileName(),
                        share.getSharedWith(),
                        permission,
                        isSuspicious
                ));
            }
        }

        result.sort(Comparator
                .comparing(OwnerShareStatusResponse::isSuspicious).reversed()
                .thenComparing(OwnerShareStatusResponse::getFileId)
                .thenComparing(OwnerShareStatusResponse::getRecipientEmail));

        return result;
    }

    // Delete file
    @DeleteMapping("/file/{id}")
    public String deleteFile(@PathVariable Long id) {

        FileEntity file = fileService.getFileById(id);

        if (file == null) {
            return "File not found";
        }

        File diskFile = new File(file.getFilePath());

        if (diskFile.exists()) {
            diskFile.delete();
        }

        fileService.deleteFile(id);

        return "File deleted successfully";
    }
    @GetMapping("/public/download/{token}")
public ResponseEntity<ByteArrayResource> publicDownload(@PathVariable String token, HttpServletRequest request) throws Exception {

    FileShare share = fileShareRepository.findByPublicToken(token);

    if(share == null){
        return ResponseEntity.notFound().build();
    }

    FileEntity file = fileService.getFileById(share.getFileId());

    if(file == null){
        return ResponseEntity.notFound().build();
    }

    enforceShareConstraints(share);

    User recipient = userRepository.findByEmail(share.getSharedWith());
    boolean recipientWasSuspicious = recipient != null && recipient.isSuspicious();
    try {
        anomalyDetectionService.validateAndTrackDownload(recipient, request);
    } catch (ResponseStatusException ex) {
        if (!recipientWasSuspicious && recipient != null && recipient.isSuspicious()) {
            activityLogService.log(
                    file.getOwnerEmail(),
                    "RECIPIENT_SUSPICIOUS",
                    "Recipient " + recipient.getEmail() + " flagged suspicious while accessing shared file "
                            + file.getFileName() + ". Reason: " + recipient.getSuspiciousReason()
            );
        }
        throw ex;
    }

    Path path = Paths.get(file.getFilePath());

    byte[] encryptedData = Files.readAllBytes(path);

    byte[] decryptedData = FileEncryptionUtil.decrypt(encryptedData);

    ByteArrayResource resource = new ByteArrayResource(decryptedData);

    String permission = share.getPermission() == null ? "VIEW" : share.getPermission().toUpperCase();
    int updatedDownloadCount = (share.getDownloadCount() == null ? 0 : share.getDownloadCount()) + 1;
    share.setDownloadCount(updatedDownloadCount);
    fileShareRepository.save(share);

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFileName() + "\"")
            .header("X-Share-Permission", permission)
            .header("X-Share-Downloads-Used", String.valueOf(updatedDownloadCount))
            .header("X-Share-Downloads-Max", share.getMaxDownloads() == null ? "UNLIMITED" : String.valueOf(share.getMaxDownloads()))
            .contentLength(decryptedData.length)
            .body(resource);
}

    @PostMapping("/public/save/{token}")
    public ResponseEntity<String> publicSave(@PathVariable String token, @RequestBody String content) throws Exception {

        FileShare share = fileShareRepository.findByPublicToken(token);

        if (share == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid share token");
        }

        enforceShareConstraints(share);

        String permission = share.getPermission() == null ? "VIEW" : share.getPermission().toUpperCase();
        if (!"EDIT".equals(permission)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Not allowed: this share has VIEW permission");
        }

        FileEntity file = fileService.getFileById(share.getFileId());
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }

        Path path = Paths.get(file.getFilePath());
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stored file not found");
        }

        byte[] updatedBytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = FileEncryptionUtil.encrypt(updatedBytes);
        Files.write(path, encryptedBytes);

        return ResponseEntity.ok("File content saved successfully");
    }

    private void enforceShareConstraints(FileShare share) {
        Instant now = Instant.now();
        Instant expiresAt = share.getExpiresAt();

        if (expiresAt != null && !now.isBefore(expiresAt)) {
            // Log suspicious activity for the owner
            FileEntity file = fileService.getFileById(share.getFileId());
            if (file != null) {
                activityLogService.log(
                    file.getOwnerEmail(),
                    "RECIPIENT_EXCEEDED_TIME_LIMIT",
                    "Recipient " + share.getSharedWith() + " attempted to access file '"
                    + (file.getFileName() != null ? file.getFileName() : ("ID " + file.getId()))
                    + "' after the share expired at " + expiresAt + "."
                );
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Share expired. Access is no longer available.");
        }

        Integer maxDownloads = share.getMaxDownloads();
        int currentDownloads = share.getDownloadCount() == null ? 0 : share.getDownloadCount();

        if (maxDownloads != null && currentDownloads >= maxDownloads) {
            // Log suspicious activity for the owner
            FileEntity file = fileService.getFileById(share.getFileId());
            if (file != null) {
                activityLogService.log(
                    file.getOwnerEmail(),
                    "RECIPIENT_EXCEEDED_DOWNLOAD_LIMIT",
                    "Recipient " + share.getSharedWith() + " attempted to download file '" +
                    (file.getFileName() != null ? file.getFileName() : ("ID " + file.getId())) +
                    "' more than the allowed maxDownloads (" + maxDownloads + ")."
                );
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Download limit reached for this share.");
        }
    }
}