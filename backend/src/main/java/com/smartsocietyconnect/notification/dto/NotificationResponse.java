package com.smartsocietyconnect.notification.dto;
import java.time.LocalDateTime;
public record NotificationResponse(Integer notificationId, String type, String title, String message, String targetPath, Boolean isRead, LocalDateTime createdAt) {}
