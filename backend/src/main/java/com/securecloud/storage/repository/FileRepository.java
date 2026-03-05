package com.securecloud.storage.repository;

import com.securecloud.storage.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByOwnerEmail(String ownerEmail);

}