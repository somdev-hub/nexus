package com.nexus.hr.service.implementations;

import com.nexus.hr.model.enums.CommunicationStatus;
import com.nexus.hr.model.enums.CommunicationType;
import com.nexus.hr.payload.EmailCommunicationDto;
import com.nexus.hr.repository.HrCommunicationRepo;
import com.nexus.hr.utils.Logger;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommunicationService Tests")
class CommunicationServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private Logger logger;

    @Mock
    private HrCommunicationRepo hrCommunicationRepo;

    @InjectMocks
    private CommunicationServiceImpl communicationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(communicationService, "defaultFromEmail", "noreply@nexushr.com");
        ReflectionTestUtils.setField(communicationService, "maxRecipients", 100);
    }

    @Test
    @DisplayName("Should send email successfully with valid input")
    void testSendEmailSuccess() throws MessagingException {
        // Arrange
        EmailCommunicationDto dto = createValidEmailDto();
        MimeMessage mockMessage = mock(MimeMessage.class);

        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("SUCCESS", responseBody.get("status"));
        assertEquals("Email sent successfully", responseBody.get("message"));
        assertEquals(2, responseBody.get("recipientCount"));

        verify(javaMailSender, times(1)).send(mockMessage);
        verify(hrCommunicationRepo, times(1)).save(any());
    }

    @Test
    @DisplayName("Should return 400 when recipient emails are missing")
    void testSendEmailWithoutRecipients() {
        // Arrange
        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setSubject("Test Subject");
        dto.setBody("Test Body");
        dto.setRecipientEmails(List.of()); // Empty recipients

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(javaMailSender, never()).send(any());
        verify(logger, times(1)).saveLogs(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return 400 when subject is empty")
    void testSendEmailWithoutSubject() {
        // Arrange
        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setRecipientEmails(List.of("user@example.com"));
        dto.setSubject(""); // Empty subject
        dto.setBody("Test Body");

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(javaMailSender, never()).send(any());
    }

    @Test
    @DisplayName("Should return 400 when body is empty")
    void testSendEmailWithoutBody() {
        // Arrange
        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setRecipientEmails(List.of("user@example.com"));
        dto.setSubject("Test Subject");
        dto.setBody(""); // Empty body

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(javaMailSender, never()).send(any());
    }

    @Test
    @DisplayName("Should return 400 for invalid email format")
    void testSendEmailWithInvalidEmail() {
        // Arrange
        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setRecipientEmails(List.of("invalid-email-format")); // Invalid email
        dto.setSubject("Test Subject");
        dto.setBody("Test Body");

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(javaMailSender, never()).send(any());
    }

    @Test
    @DisplayName("Should return 400 when exceeding max recipients")
    void testSendEmailExceedsMaxRecipients() {
        // Arrange
        EmailCommunicationDto dto = new EmailCommunicationDto();
        List<String> tooManyRecipients = new java.util.ArrayList<>();
        for (int i = 0; i < 101; i++) {
            tooManyRecipients.add("user" + i + "@example.com");
        }
        dto.setRecipientEmails(tooManyRecipients);
        dto.setSubject("Test Subject");
        dto.setBody("Test Body");

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(javaMailSender, never()).send(any());
    }

    @Test
    @DisplayName("Should handle invalid email format in CC list")
    void testSendEmailWithInvalidCCFormat() {
        // Arrange
        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setRecipientEmails(List.of("user@example.com"));
        dto.setSubject("Test Subject");
        dto.setBody("Test Body");
        dto.setCcEmails(List.of("invalid-cc-format")); // Invalid CC email

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(javaMailSender, never()).send(any());
    }

    @Test
    @DisplayName("Should handle invalid email format in BCC list")
    void testSendEmailWithInvalidBCCFormat() {
        // Arrange
        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setRecipientEmails(List.of("user@example.com"));
        dto.setSubject("Test Subject");
        dto.setBody("Test Body");
        dto.setBccEmails(List.of("invalid-bcc@")); // Invalid BCC email

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(javaMailSender, never()).send(any());
    }

    @Test
    @DisplayName("Should use default from email when sender email is null")
    void testSendEmailWithDefaultFromEmail() throws MessagingException {
        // Arrange
        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setRecipientEmails(List.of("user@example.com"));
        dto.setSubject("Test Subject");
        dto.setBody("Test Body");
        dto.setSenderEmail(null); // No sender email

        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(javaMailSender, times(1)).send(mockMessage);
    }

    @Test
    @DisplayName("Should send email with CC and BCC recipients")
    void testSendEmailWithCCAndBCC() throws MessagingException {
        // Arrange
        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setRecipientEmails(List.of("user@example.com"));
        dto.setSubject("Test Subject");
        dto.setBody("Test Body");
        dto.setCcEmails(List.of("cc@example.com", "cc2@example.com"));
        dto.setBccEmails(List.of("bcc@example.com"));

        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(javaMailSender, times(1)).send(mockMessage);
    }

    @Test
    @DisplayName("Should handle null input gracefully")
    void testSendEmailWithNullInput() {
        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(javaMailSender, never()).send(any());
    }

    @Test
    @DisplayName("Should log communication to database on success")
    void testLogCommunicationToDatabaseOnSuccess() throws MessagingException {
        // Arrange
        EmailCommunicationDto dto = createValidEmailDto();
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);

        // Act
        communicationService.sendCommunicationOverEmail(dto);

        // Assert
        verify(hrCommunicationRepo, times(1)).save(argThat(communication ->
            communication.getCommunicationType() == CommunicationType.EMAIL &&
            communication.getStatus() == CommunicationStatus.SENT &&
            communication.getSubject().equals("Test Subject")
        ));
    }

    @Test
    @DisplayName("Should log communication to database on failure")
    void testLogCommunicationToDatabaseOnFailure() throws MessagingException {
        // Arrange
        EmailCommunicationDto dto = createValidEmailDto();
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        doThrow(new MessagingException("SMTP Error")).when(javaMailSender).send(any());

        // Act
        communicationService.sendCommunicationOverEmail(dto);

        // Assert
        verify(hrCommunicationRepo, times(1)).save(argThat(communication ->
            communication.getStatus() == CommunicationStatus.FAILED
        ));
    }

    @Test
    @DisplayName("Should return success response with correct structure")
    void testSuccessResponseStructure() throws MessagingException {
        // Arrange
        EmailCommunicationDto dto = createValidEmailDto();
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);

        // Act
        ResponseEntity<?> response = communicationService.sendCommunicationOverEmail(dto);

        // Assert
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("message"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("recipientCount"));
        assertTrue(body.containsKey("subject"));
        assertTrue(body.containsKey("hasAttachments"));
    }

    // Helper method to create valid EmailCommunicationDto
    private EmailCommunicationDto createValidEmailDto() {
        EmailCommunicationDto dto = new EmailCommunicationDto();
        dto.setSenderEmail("sender@example.com");
        dto.setRecipientEmails(List.of("recipient1@example.com", "recipient2@example.com"));
        dto.setSubject("Test Subject");
        dto.setBody("<html><body><p>Test Body</p></body></html>");
        return dto;
    }
}
