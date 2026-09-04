package com.smartsocietyconnect.societydocument.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smartsocietyconnect.societydocument.entity.SocietyDocument;
public interface SocietyDocumentRepository extends JpaRepository<SocietyDocument, Integer> {
    List<SocietyDocument> findAllByOrderByCreatedAtDesc();
    List<SocietyDocument> findByPublishedTrueOrderByCreatedAtDesc();
}
