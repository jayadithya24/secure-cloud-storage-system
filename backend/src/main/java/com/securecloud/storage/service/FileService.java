package com.securecloud.storage.service;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.securecloud.storage.model.FileEntity;
import com.securecloud.storage.repository.FileRepository;
import com.securecloud.storage.util.FileEncryptionUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    // Get all files uploaded by a user
    public List<FileEntity> getFilesByUser(String email) {
        return fileRepository.findByOwnerEmail(email);
    }

    // Upload file with encryption
    public FileEntity uploadFile(MultipartFile file, String email) throws Exception {

        String uploadDir = System.getProperty("user.dir") + "/uploads/";

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String path = uploadDir + file.getOriginalFilename();

        // Convert file to bytes
        byte[] fileBytes = file.getBytes();

        // Encrypt file
        byte[] encryptedBytes = FileEncryptionUtil.encrypt(fileBytes);

        // Save encrypted file
        Files.write(new File(path).toPath(), encryptedBytes);

        // Save metadata in database
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setFilePath(path);
        fileEntity.setOwnerEmail(email);

        return fileRepository.save(fileEntity);
    }

    // Get file by ID
    public FileEntity getFileById(Long id) {
        return fileRepository.findById(id).orElse(null);
    }

    // Delete file
    public void deleteFile(Long id) {

        FileEntity file = fileRepository.findById(id).orElse(null);

        if (file != null) {
            File diskFile = new File(file.getFilePath());

            if (diskFile.exists()) {
                diskFile.delete();
            }

            fileRepository.deleteById(id);
        }
    }
}