package com.smartsocietyconnect.staffdocument.entity;
import java.time.*;
import org.hibernate.annotations.CreationTimestamp;
import com.smartsocietyconnect.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="staff_documents") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffDocument { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="staff_document_id") private Integer staffDocumentId; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="staff_user_id", nullable=false) private User staffUser; @Column(nullable=false,length=40) private String documentType; @Column(nullable=false,length=150) private String documentName; @Column(name="file_path",nullable=false,length=255) private String filePath; private LocalDate expiryDate; @Column(nullable=false,length=20) @Builder.Default private String verificationStatus="PENDING"; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="uploaded_by",nullable=false) private User uploadedBy; @CreationTimestamp @Column(name="created_at",updatable=false) private LocalDateTime createdAt; }
