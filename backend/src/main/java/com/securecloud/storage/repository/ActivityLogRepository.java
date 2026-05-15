package com.securecloud.storage.repository;

import com.securecloud.storage.model.ActivityLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    List<ActivityLog> findByActorEmailOrderByCreatedAtDesc(String actorEmail);
}