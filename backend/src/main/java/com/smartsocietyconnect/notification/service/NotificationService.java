package com.smartsocietyconnect.notification.service;
import java.util.Collection;
import java.util.List;
import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.notification.dto.NotificationResponse;
public interface NotificationService {
    void notifyUser(User recipient, String type, String title, String message, String targetPath);
    void notifyUsers(Collection<User> recipients, String type, String title, String message, String targetPath);
    List<NotificationResponse> getMyNotifications();
    void markRead(Integer notificationId);
    void markAllRead();
}
