package com.smartsocietyconnect.societydocument.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import com.smartsocietyconnect.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "society_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocietyDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "society_document_id") private Integer societyDocumentId;
    @Column(nullable = false, length = 40) private String category;
    @Column(nullable = false, length = 150) private String title;
    @Column(length = 120) private String vendor;
    @Column(name = "billing_month", length = 20) private String billingMonth;
    @Column(precision = 12, scale = 2) private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    @Column(name = "file_path", nullable = false, length = 255) private String filePath;
    @Builder.Default @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0") private Boolean published = false;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "uploaded_by", nullable = false) private User uploadedBy;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
