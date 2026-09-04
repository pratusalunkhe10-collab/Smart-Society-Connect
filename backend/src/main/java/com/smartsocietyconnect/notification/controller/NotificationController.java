package com.smartsocietyconnect.notification.controller;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartsocietyconnect.notification.dto.NotificationResponse;
import com.smartsocietyconnect.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    @GetMapping public ResponseEntity<List<NotificationResponse>> getMine() { return ResponseEntity.ok(notificationService.getMyNotifications()); }
    @PatchMapping("/{notificationId}/read") public ResponseEntity<Void> markRead(@PathVariable Integer notificationId) { notificationService.markRead(notificationId); return ResponseEntity.noContent().build(); }
    @PatchMapping("/read-all") public ResponseEntity<Void> markAllRead() { notificationService.markAllRead(); return ResponseEntity.noContent().build(); }
}
