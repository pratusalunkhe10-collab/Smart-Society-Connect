package com.smartsocietyconnect.notification.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smartsocietyconnect.notification.entity.UserNotification;
public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {
    List<UserNotification> findByRecipientUserIdOrderByCreatedAtDesc(Integer userId);
    boolean existsByRecipientUserIdAndTypeAndTitle(Integer userId, String type, String title);
}
