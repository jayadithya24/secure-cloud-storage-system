package com.securecloud.storage.repository;

import com.securecloud.storage.model.FileEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FileRepository extends MongoRepository<FileEntity, String> {

    List<FileEntity> findByOwnerEmail(String ownerEmail);

}