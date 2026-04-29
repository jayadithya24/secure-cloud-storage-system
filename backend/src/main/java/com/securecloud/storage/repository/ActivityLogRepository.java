package com.securecloud.storage.repository;

import com.securecloud.storage.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
	List<ActivityLog> findByActorEmailOrderByCreatedAtDesc(String actorEmail);
}