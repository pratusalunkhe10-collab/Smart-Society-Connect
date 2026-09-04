package com.smartsocietyconnect.billing.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.smartsocietyconnect.billing.entity.Billing;
import com.smartsocietyconnect.billing.enums.BillingStatus;
import com.smartsocietyconnect.billing.repository.BillingRepository;
import com.smartsocietyconnect.notification.repository.UserNotificationRepository;
import com.smartsocietyconnect.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sends one private reminder for each bill due in three days. */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillingReminderScheduler {
    private final BillingRepository billingRepository;
    private final UserNotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.billing.reminder-cron:0 0 9 * * *}")
    public void sendUpcomingDueReminders() {
        LocalDate dueDate = LocalDate.now().plusDays(3);
        billingRepository.findByDueDateAndStatusIn(dueDate, List.of(BillingStatus.PENDING, BillingStatus.PARTIALLY_PAID))
                .forEach(this::notifyResidentOnce);
    }

    private void notifyResidentOnce(Billing bill) {
        String title = "Maintenance bill due in 3 days";
        Integer userId = bill.getResident().getUser().getUserId();
        if (notificationRepository.existsByRecipientUserIdAndTypeAndTitle(userId, "BILL_DUE", title + " #" + bill.getBillingId())) return;
        notificationService.notifyUser(bill.getResident().getUser(), "BILL_DUE", title + " #" + bill.getBillingId(),
                "₹" + bill.getTotalAmount() + " is due on " + bill.getDueDate() + ". Please pay before the deadline.",
                "/app/billing");
        log.info("Sent billing due reminder. billingId={}", bill.getBillingId());
    }
}
