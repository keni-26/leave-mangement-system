package com.elms.service;

import com.elms.entity.Employee;
import com.elms.entity.Notification;
import com.elms.entity.NotificationType;
import com.elms.entity.User;
import com.elms.repository.NotificationRepository;
import com.elms.repository.UserRepository;
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

    public List<Notification> getNotificationsForUser(Long userId) {
        User user = getUser(userId);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Notification> getUnreadNotificationsForUser(Long userId) {
        User user = getUser(userId);
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    @Transactional
    public Notification markNotificationAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllNotificationsAsRead(Long userId) {
        User user = getUser(userId);
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
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

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }
}
