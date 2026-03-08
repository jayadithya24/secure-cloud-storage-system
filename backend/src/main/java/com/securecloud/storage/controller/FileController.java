package com.securecloud.storage.controller;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.nio.file.Files;

import com.securecloud.storage.model.FileEntity;
import com.securecloud.storage.model.FileShare;
import com.securecloud.storage.dto.SharedFileResponse;
import com.securecloud.storage.repository.FileShareRepository;
import com.securecloud.storage.repository.FileRepository;
import com.securecloud.storage.service.FileService;
import com.securecloud.storage.util.FileEncryptionUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;

@RestController
@RequestMapping("/api")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileShareRepository fileShareRepository;

    @Autowired
    private FileRepository fileRepository;

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
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long id) throws Exception {

        FileEntity fileEntity = fileService.getFileById(id);

        if (fileEntity == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(fileEntity.getFilePath());

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

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
public String shareFile(
        @RequestParam Long fileId,
        @RequestParam String email
) {

    if(fileShareRepository.existsByFileIdAndSharedWith(fileId, email)){
        return "File already shared with this user";
    }

    String token = UUID.randomUUID().toString();

    FileShare share = new FileShare();
    share.setFileId(fileId);
    share.setSharedWith(email);
    share.setPublicToken(token);

    fileShareRepository.save(share);

    return "Share link: http://localhost:8080/api/public/download/" + token;
}

    // View shared files
    @GetMapping("/shared-files")
    public List<SharedFileResponse> getSharedFiles(@RequestParam String email) {

        List<FileShare> shares = fileShareRepository.findBySharedWith(email);

        List<SharedFileResponse> result = new ArrayList<>();

        for (FileShare share : shares) {

            FileEntity file = fileRepository.findById(share.getFileId()).orElse(null);

            if (file != null) {
                result.add(new SharedFileResponse(
                        file.getFileName(),
                        file.getOwnerEmail()
                ));
            }
        }

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
public ResponseEntity<ByteArrayResource> publicDownload(@PathVariable String token) throws Exception {

    FileShare share = fileShareRepository.findByPublicToken(token);

    if(share == null){
        return ResponseEntity.notFound().build();
    }

    FileEntity file = fileService.getFileById(share.getFileId());

    if(file == null){
        return ResponseEntity.notFound().build();
    }

    Path path = Paths.get(file.getFilePath());

    byte[] encryptedData = Files.readAllBytes(path);

    byte[] decryptedData = FileEncryptionUtil.decrypt(encryptedData);

    ByteArrayResource resource = new ByteArrayResource(decryptedData);

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + file.getFileName() + "\"")
            .contentLength(decryptedData.length)
            .body(resource);
}
}