package com.securecloud.storage.service;

import com.securecloud.storage.model.ActivityLog;
import com.securecloud.storage.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    public void log(String actorEmail, String action, String details) {
        ActivityLog activityLog = new ActivityLog();
        activityLog.setActorEmail(actorEmail);
        activityLog.setAction(action);
        activityLog.setDetails(details);
        activityLogRepository.save(activityLog);
    }
}