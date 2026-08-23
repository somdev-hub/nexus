package com.nexus.hr.controller;

import com.nexus.hr.payload.EmailCommunicationDto;
import com.nexus.hr.service.interfaces.CommunicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/communication")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationService communicationService;

    /**
     * Send email communication to one or multiple recipients
     *
     * @param emailCommunicationDto Email communication request containing:
     *        - senderEmail: Optional sender email (defaults to configured noreply email)
     *        - recipientEmails: List of recipient email addresses (required, max 100)
     *        - subject: Email subject (required)
     *        - body: Email body content - supports HTML (required)
     *        - ccEmails: Optional list of CC email addresses
     *        - bccEmails: Optional list of BCC email addresses
     *        - attachments: Optional list of attachments with file URLs
     *
     * @return ResponseEntity with success/error response
     *
     * @example
     * POST /api/v1/communication/send-email
     * {
     *   "senderEmail": "hr@company.com",
     *   "recipientEmails": ["employee@company.com", "manager@company.com"],
     *   "subject": "Offer Letter - Software Engineer",
     *   "body": "<html><body><p>Congratulations on your offer!</p></body></html>",
     *   "ccEmails": ["recruiter@company.com"],
     *   "bccEmails": ["admin@company.com"],
     *   "attachments": [
     *     {
     *       "fileName": "joining-letter.pdf",
     *       "contentType": "application/pdf",
     *       "fileUrl": "http://dms-service/documents/joining-letter-123.pdf"
     *     }
     *   ]
     * }
     */
    @PostMapping("/send-email")
    public ResponseEntity<?> sendEmail(@RequestBody EmailCommunicationDto emailCommunicationDto) {
        log.info("Received email communication request for {} recipients",
            emailCommunicationDto.getRecipientEmails().size());
        return communicationService.sendCommunicationOverEmail(emailCommunicationDto);
    }

    /**
     * Send email to a single recipient
     *
     * @param recipientEmail Single recipient email address
     * @param subject Email subject
     * @param body Email body (supports HTML)
     * @return ResponseEntity with success/error response
     *
     * @example
     * POST /api/v1/communication/send-email/simple?recipientEmail=test@company.com
     * Query Parameters:
     * - recipientEmail: test@company.com
     * - subject: Joining Letter
     * - body: <html>Welcome to the team!</html>
     */
    @PostMapping("/send-email/simple")
    public ResponseEntity<?> sendSimpleEmail(
            @RequestParam String recipientEmail,
            @RequestParam String subject,
            @RequestParam String body) {

        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setRecipientEmails(java.util.List.of(recipientEmail));
        dto.setSubject(subject);
        dto.setBody(body);

        log.info("Sending simple email to {}", recipientEmail);
        return communicationService.sendCommunicationOverEmail(dto);
    }
}
