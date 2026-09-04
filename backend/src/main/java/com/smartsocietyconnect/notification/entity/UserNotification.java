package com.smartsocietyconnect.notification.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.smartsocietyconnect.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

/** A private in-app notification belonging to one user. */
@Entity
@Table(name = "user_notifications", indexes = @Index(name = "idx_notification_recipient_read", columnList = "recipient_id,is_read"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "notification_id")
    private Integer notificationId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
    @Column(nullable = false, length = 40) private String type;
    @Column(nullable = false, length = 140) private String title;
    @Column(nullable = false, length = 1000) private String message;
    @Column(name = "target_path", length = 160) private String targetPath;
    @Builder.Default @Column(name = "is_read", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isRead = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
