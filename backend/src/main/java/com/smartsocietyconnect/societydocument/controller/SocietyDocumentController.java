package com.smartsocietyconnect.societydocument.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.resident.service.FileStorageService;
import com.smartsocietyconnect.societydocument.entity.SocietyDocument;
import com.smartsocietyconnect.societydocument.repository.SocietyDocumentRepository;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/society-documents") @RequiredArgsConstructor
public class SocietyDocumentController {
    private final SocietyDocumentRepository repository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @GetMapping public List<Response> list() { return (isStaff() ? repository.findAllByOrderByCreatedAtDesc() : repository.findByPublishedTrueOrderByCreatedAtDesc()).stream().map(this::toResponse).toList(); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response> upload(@RequestParam String category, @RequestParam String title, @RequestParam MultipartFile file,
            @RequestParam(required = false) String vendor, @RequestParam(required = false) String billingMonth,
            @RequestParam(required = false) BigDecimal amount, @RequestParam(required = false) LocalDate dueDate,
            @RequestParam(required = false) LocalDate paidDate, @RequestParam(defaultValue = "false") Boolean published) {
        requireUploadPermission(category);
        SocietyDocument document = SocietyDocument.builder().category(category).title(title).vendor(vendor).billingMonth(billingMonth)
                .amount(amount).dueDate(dueDate).paidDate(paidDate).published(published).filePath(fileStorageService.storeFile(file)).uploadedBy(currentUser()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(document)));
    }

    @PatchMapping("/{id}/publish") public Response publish(@PathVariable Integer id, @RequestParam Boolean published) { requireManager(); SocietyDocument document = get(id); document.setPublished(published); return toResponse(repository.save(document)); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Integer id) { requireManager(); SocietyDocument document = get(id); repository.delete(document); fileStorageService.deleteFile(document.getFilePath()); }
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Integer id) {
        SocietyDocument document = get(id);
        if (!Boolean.TRUE.equals(document.getPublished()) && !isStaff() && !isUploader(document)) {
            throw new IllegalArgumentException("This document is private.");
        }
        Resource resource = fileStorageService.loadFile(document.getFilePath());
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        String filename = resource.getFilename() == null ? document.getTitle() : resource.getFilename();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(filename)
                        .build().toString())
                .body(resource);
    }

    private SocietyDocument get(Integer id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Society document not found.")); }
    private boolean isStaff() { return hasAnyAuthority("ADMIN", "SECRETARY", "ACCOUNTANT"); }
    private void requireManager() { if (!isManager()) throw new IllegalArgumentException("Only Admin or Secretary can manage society documents."); }
    private boolean isManager() { return hasAnyAuthority("ADMIN", "SECRETARY"); }
    private boolean isAccountant() { return hasAnyAuthority("ACCOUNTANT"); }
    private boolean isUploader(SocietyDocument document) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && document.getUploadedBy() != null
                && document.getUploadedBy().getEmail().equalsIgnoreCase(authentication.getName());
    }
    private boolean hasAnyAuthority(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        List<String> allowed = List.of(roles);
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .anyMatch(allowed::contains);
    }
    private void requireUploadPermission(String category) { if (isManager()) return; if (isAccountant() && List.of("UTILITY_BILL", "VENDOR_INVOICE", "UTILITY BILL", "VENDOR INVOICE").contains(category)) return; throw new IllegalArgumentException("Accountants can upload only utility bills and vendor invoices."); }
    private User currentUser() { Authentication a = SecurityContextHolder.getContext().getAuthentication(); return userRepository.findByEmail(a.getName()).orElseThrow(() -> new IllegalArgumentException("Authenticated user not found.")); }
    private Response toResponse(SocietyDocument d) { return new Response(d.getSocietyDocumentId(), d.getCategory(), d.getTitle(), d.getVendor(), d.getBillingMonth(), d.getAmount(), d.getDueDate(), d.getPaidDate(), d.getPublished(), d.getCreatedAt()); }
    public record Response(Integer id, String category, String title, String vendor, String billingMonth, BigDecimal amount, LocalDate dueDate, LocalDate paidDate, Boolean published, LocalDateTime createdAt) {}
}
