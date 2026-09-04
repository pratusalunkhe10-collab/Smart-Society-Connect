package com.smartsocietyconnect.staffdocument.repository;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import com.smartsocietyconnect.staffdocument.entity.StaffDocument;
public interface StaffDocumentRepository extends JpaRepository<StaffDocument,Integer> { List<StaffDocument> findByStaffUserUserIdOrderByCreatedAtDesc(Integer userId); List<StaffDocument> findAllByOrderByCreatedAtDesc(); }
