package com.securecloud.storage.repository;

import com.securecloud.storage.model.UserActivity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;

public interface UserActivityRepository extends MongoRepository<UserActivity, String> {

    UserActivity findTopByUserIdAndActionOrderByTimestampDesc(String userId, String action);

    long countByUserIdAndActionAndTimestampAfter(String userId, String action, Instant threshold);
}
