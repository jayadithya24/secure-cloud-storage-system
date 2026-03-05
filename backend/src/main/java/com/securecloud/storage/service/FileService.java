package com.securecloud.storage.service;

import java.util.List;
import com.securecloud.storage.model.FileEntity;
import com.securecloud.storage.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    public List<FileEntity> getFilesByUser(String email) {
    return fileRepository.findByOwnerEmail(email);
}

    public FileEntity uploadFile(MultipartFile file, String email) throws IOException {

        String uploadDir = System.getProperty("user.dir") + "/uploads/";

        File dir = new File(uploadDir);
        if(!dir.exists()){
            dir.mkdirs();
        }

        String path = uploadDir + file.getOriginalFilename();

        file.transferTo(new File(path));

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setFilePath(path);
        fileEntity.setOwnerEmail(email);

        return fileRepository.save(fileEntity);
    }
}