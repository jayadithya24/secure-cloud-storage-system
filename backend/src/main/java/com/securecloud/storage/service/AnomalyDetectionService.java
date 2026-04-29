package com.securecloud.storage.service;

import com.securecloud.storage.dto.SuspiciousUserResponse;
import com.securecloud.storage.model.User;
import com.securecloud.storage.model.UserActivity;
import com.securecloud.storage.repository.UserActivityRepository;
import com.securecloud.storage.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class AnomalyDetectionService {

    private static final String ACTION_LOGIN = "LOGIN";
    private static final String ACTION_DOWNLOAD = "DOWNLOAD";
    private static final String RESTRICTED_MESSAGE = "Suspicious activity detected. Access temporarily restricted.";

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private UserRepository userRepository;

    public void trackLogin(User user, HttpServletRequest request) {
        if (user == null || user.getId() == null) {
            return;
        }

        String ipAddress = extractClientIp(request);
        UserActivity previousLogin = userActivityRepository
                .findTopByUserIdAndActionOrderByTimestampDesc(user.getId(), ACTION_LOGIN);

        if (previousLogin != null && !previousLogin.getIpAddress().equals(ipAddress)) {
            markSuspicious(user, "Login from different IP. Previous=" + previousLogin.getIpAddress() + ", Current=" + ipAddress);
        }

        saveActivity(user.getId(), ipAddress, ACTION_LOGIN);
    }

    public void validateAndTrackDownload(User user, HttpServletRequest request) {
        if (user == null || user.getId() == null) {
            return;
        }

        if (user.isSuspicious()) {
            throw new ResponseStatusException(FORBIDDEN, RESTRICTED_MESSAGE);
        }

        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        long recentDownloadCount = userActivityRepository
                .countByUserIdAndActionAndTimestampAfter(user.getId(), ACTION_DOWNLOAD, oneMinuteAgo);

        if (recentDownloadCount >= 5) {
            markSuspicious(user, "More than 5 downloads within 1 minute");
            throw new ResponseStatusException(FORBIDDEN, RESTRICTED_MESSAGE);
        }

        saveActivity(user.getId(), extractClientIp(request), ACTION_DOWNLOAD);
    }

    public List<SuspiciousUserResponse> getSuspiciousUsers() {
        return userRepository.findByIsSuspiciousTrueOrderByIdAsc().stream()
                .map(user -> new SuspiciousUserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.isSuspicious(),
                        user.getSuspiciousReason(),
                        user.getSuspiciousAt()
                ))
                .toList();
    }

    private void markSuspicious(User user, String reason) {
        user.setSuspicious(true);
        user.setSuspiciousReason(reason);
        user.setSuspiciousAt(Instant.now());
        userRepository.save(user);
    }

    private void saveActivity(Long userId, String ipAddress, String action) {
        UserActivity activity = new UserActivity();
        activity.setUserId(userId);
        activity.setIpAddress(ipAddress);
        activity.setAction(action);
        userActivityRepository.save(activity);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
