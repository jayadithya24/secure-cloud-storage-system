package com.securecloud.storage.controller;

import com.securecloud.storage.model.ActivityLog;
import com.securecloud.storage.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping("/alerts")
    public List<ActivityLog> getAlerts(@RequestParam String email) {
        return activityLogService.getLogsForActor(email);
    }
}
