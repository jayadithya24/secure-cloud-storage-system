package com.securecloud.storage.controller;

import java.util.List;
import com.securecloud.storage.model.FileEntity;
import com.securecloud.storage.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public FileEntity upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("email") String email
    ) throws Exception {
        return fileService.uploadFile(file, email);
    }

    @GetMapping("/files")
    public List<FileEntity> getFiles(@RequestParam String email) {
        return fileService.getFilesByUser(email);
    }
}