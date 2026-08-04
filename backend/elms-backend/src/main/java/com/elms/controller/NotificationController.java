package com.elms.controller;

import com.elms.dto.NotificationResponse;
import com.elms.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsForUser(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId, authentication.getName()));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotificationsForUser(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationsForUser(userId, authentication.getName()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markNotificationAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.markNotificationAsRead(id, authentication.getName()));
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(@PathVariable Long userId, Authentication authentication) {
        notificationService.markAllNotificationsAsRead(userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
