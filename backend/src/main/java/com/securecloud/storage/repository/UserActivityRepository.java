package com.securecloud.storage.repository;

import com.securecloud.storage.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    UserActivity findTopByUserIdAndActionOrderByTimestampDesc(Long userId, String action);

    long countByUserIdAndActionAndTimestampAfter(Long userId, String action, Instant threshold);
}
