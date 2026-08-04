package com.elms.service;

import com.elms.entity.Employee;
import com.elms.entity.Notification;
import com.elms.entity.NotificationType;
import com.elms.entity.User;
import com.elms.dto.NotificationResponse;
import com.elms.exception.ResourceNotFoundException;
import com.elms.repository.NotificationRepository;
import com.elms.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<NotificationResponse> getNotificationsForUser(Long userId, String authenticatedEmail) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        ensureOwnUserId(authenticatedUser, userId);
        return notificationRepository.findByUserOrderByCreatedAtDesc(authenticatedUser).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotificationsForUser(Long userId, String authenticatedEmail) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        ensureOwnUserId(authenticatedUser, userId);
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(authenticatedUser).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markNotificationAsRead(Long id, String authenticatedEmail) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        if (!authenticatedUser.getId().equals(notification.getUser().getId())) {
            throw new AccessDeniedException("You are not authorized to access this notification");
        }
        notification.setIsRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllNotificationsAsRead(Long userId, String authenticatedEmail) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        ensureOwnUserId(authenticatedUser, userId);
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(authenticatedUser);
        for (Notification notification : notifications) {
            if (!Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
            }
        }
    }

    @Transactional
    public void createNotification(User user, NotificationType type, String message) {
        if (user == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createNotificationForEmployee(Employee employee, NotificationType type, String message) {
        if (employee == null || employee.getUser() == null) {
            return;
        }
        createNotification(employee.getUser(), type, message);
    }

    private User getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    private void ensureOwnUserId(User authenticatedUser, Long requestedUserId) {
        if (!authenticatedUser.getId().equals(requestedUserId)) {
            throw new AccessDeniedException("Users can only access their own notifications");
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
