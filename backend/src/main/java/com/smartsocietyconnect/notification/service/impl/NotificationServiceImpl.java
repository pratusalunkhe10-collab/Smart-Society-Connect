package com.smartsocietyconnect.notification.service.impl;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.notification.dto.NotificationResponse;
import com.smartsocietyconnect.notification.entity.UserNotification;
import com.smartsocietyconnect.notification.repository.UserNotificationRepository;
import com.smartsocietyconnect.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor @Transactional
public class NotificationServiceImpl implements NotificationService {
    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    @Override public void notifyUser(User recipient, String type, String title, String message, String targetPath) {
        if (recipient == null || recipient.getUserId() == null) return;
        notificationRepository.save(UserNotification.builder().recipient(recipient).type(type).title(title).message(message).targetPath(targetPath).build());
    }
    @Override public void notifyUsers(Collection<User> recipients, String type, String title, String message, String targetPath) {
        recipients.stream().filter(user -> user != null && user.getUserId() != null).collect(java.util.stream.Collectors.toMap(User::getUserId, user -> user, (first, ignored) -> first)).values().forEach(user -> notifyUser(user, type, title, message, targetPath));
    }
    @Override @Transactional(readOnly = true) public List<NotificationResponse> getMyNotifications() {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(currentUser().getUserId()).stream().map(this::toResponse).toList();
    }
    @Override public void markRead(Integer notificationId) {
        UserNotification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new IllegalArgumentException("Notification not found."));
        if (!notification.getRecipient().getUserId().equals(currentUser().getUserId())) throw new IllegalArgumentException("You cannot update this notification.");
        notification.setIsRead(true);
    }
    @Override public void markAllRead() { notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(currentUser().getUserId()).forEach(notification -> notification.setIsRead(true)); }
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) throw new IllegalStateException("Authenticated user not found.");
        return userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }
    private NotificationResponse toResponse(UserNotification n) { return new NotificationResponse(n.getNotificationId(), n.getType(), n.getTitle(), n.getMessage(), n.getTargetPath(), n.getIsRead(), n.getCreatedAt()); }
}
