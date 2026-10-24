package com.nexus.cms.notification.controller;

import com.nexus.cms.notification.entities.NotificationPolicy;
import com.nexus.cms.notification.repositories.NotificationPolicyRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * REST controller for notification policy (rule) CRUD.
 * <p>
 * Allows super-admins to view, create, update, and deactivate notification
 * rules without a code release.
 */
@RestController
@RequestMapping("/api/notifications/policies")
@RequiredArgsConstructor
@Slf4j
public class NotificationPolicyController {

    private final NotificationPolicyRepo repo;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String eventType) {
        Page<NotificationPolicy> result;
        if (eventType != null && !eventType.isBlank()) {
            result = repo.findByEventTypeOrderByNewStatusAsc(eventType, PageRequest.of(page, size));
        } else {
            result = repo.findAllByOrderByEventTypeAscNewStatusAsc(PageRequest.of(page, size));
        }

        List<Map<String, Object>> content = new ArrayList<>();
        for (NotificationPolicy p : result.getContent()) {
            content.add(toMap(p));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("page", result.getNumber());
        response.put("size", result.getSize());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(p -> ResponseEntity.ok(toMap(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        NotificationPolicy p = fromMap(body);
        Timestamp now = Timestamp.from(Instant.now());
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        p.setCreatedBy("admin");
        if (p.getIsActive() == null) p.setIsActive(true);
        NotificationPolicy saved = repo.save(p);
        return ResponseEntity.ok(toMap(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(existing -> {
            updateFromMap(existing, body);
            existing.setUpdatedAt(Timestamp.from(Instant.now()));
            existing.setUpdatedBy("admin");
            NotificationPolicy saved = repo.save(existing);
            return ResponseEntity.ok(toMap(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        return repo.findById(id).map(existing -> {
            existing.setIsActive(false);
            existing.setUpdatedAt(Timestamp.from(Instant.now()));
            existing.setUpdatedBy("admin");
            repo.save(existing);
            return ResponseEntity.ok(toMap(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/event-types")
    public ResponseEntity<List<String>> eventTypes() {
        return ResponseEntity.ok(List.of(
                "PURCHASE_ORDER_STATUS_CHANGED",
                "QUOTATION_STATUS_CHANGED",
                "PARTNERSHIP_STATUS_CHANGED",
                "INVOICE_STATUS_CHANGED",
                "PAYMENT_STATUS_CHANGED",
                "SHIPMENT_STATUS_CHANGED",
                "GOODS_RECEIPT_STATUS_CHANGED"
        ));
    }

    private Map<String, Object> toMap(NotificationPolicy p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("policyId", p.getPolicyId());
        m.put("eventType", p.getEventType());
        m.put("previousStatus", p.getPreviousStatus());
        m.put("newStatus", p.getNewStatus());
        m.put("severity", p.getSeverity());
        m.put("addressing", p.getAddressing());
        m.put("addressTo", p.getAddressTo());
        m.put("titleTemplate", p.getTitleTemplate());
        m.put("bodyTemplate", p.getBodyTemplate());
        m.put("isActive", p.getIsActive());
        m.put("description", p.getDescription());
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        return m;
    }

    private NotificationPolicy fromMap(Map<String, Object> body) {
        NotificationPolicy p = new NotificationPolicy();
        p.setEventType((String) body.get("eventType"));
        p.setPreviousStatus((String) body.get("previousStatus"));
        p.setNewStatus((String) body.get("newStatus"));
        p.setSeverity((String) body.getOrDefault("severity", "INFO"));
        p.setAddressing((String) body.getOrDefault("addressing", "ORG"));
        p.setAddressTo((String) body.getOrDefault("addressTo", "COUNTERPARTY"));
        p.setTitleTemplate((String) body.get("titleTemplate"));
        p.setBodyTemplate((String) body.get("bodyTemplate"));
        p.setDescription((String) body.get("description"));
        if (body.get("isActive") != null) {
            p.setIsActive(Boolean.valueOf(body.get("isActive").toString()));
        }
        return p;
    }

    private void updateFromMap(NotificationPolicy existing, Map<String, Object> body) {
        if (body.containsKey("eventType")) existing.setEventType((String) body.get("eventType"));
        if (body.containsKey("previousStatus")) existing.setPreviousStatus((String) body.get("previousStatus"));
        if (body.containsKey("newStatus")) existing.setNewStatus((String) body.get("newStatus"));
        if (body.containsKey("severity")) existing.setSeverity((String) body.get("severity"));
        if (body.containsKey("addressing")) existing.setAddressing((String) body.get("addressing"));
        if (body.containsKey("addressTo")) existing.setAddressTo((String) body.get("addressTo"));
        if (body.containsKey("titleTemplate")) existing.setTitleTemplate((String) body.get("titleTemplate"));
        if (body.containsKey("bodyTemplate")) existing.setBodyTemplate((String) body.get("bodyTemplate"));
        if (body.containsKey("description")) existing.setDescription((String) body.get("description"));
        if (body.containsKey("isActive")) existing.setIsActive(Boolean.valueOf(body.get("isActive").toString()));
    }
}