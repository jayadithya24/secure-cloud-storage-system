package com.securecloud.storage.repository;

import com.securecloud.storage.model.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    List<FileShare> findBySharedWith(String email);

    boolean existsByFileIdAndSharedWith(Long fileId, String sharedWith);

    FileShare findByPublicToken(String token);
}