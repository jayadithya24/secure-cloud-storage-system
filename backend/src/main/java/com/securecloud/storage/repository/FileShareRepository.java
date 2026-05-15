package com.securecloud.storage.repository;

import com.securecloud.storage.model.FileShare;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface FileShareRepository extends MongoRepository<FileShare, String> {

    List<FileShare> findBySharedWith(String email);

    boolean existsByFileIdAndSharedWith(String fileId, String sharedWith);

    FileShare findByFileIdAndSharedWith(String fileId, String sharedWith);

    FileShare findByPublicToken(String token);

    List<FileShare> findByFileId(String fileId);
}