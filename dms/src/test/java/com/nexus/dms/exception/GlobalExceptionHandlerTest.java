package com.nexus.dms.exception;

import com.nexus.dms.dto.ErrorResponseDto;
import com.nexus.dms.dto.FileValidationExceptionDto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
    }

    @Test
    void testHandleIllegalArgumentException() {
        String errorMessage = "Invalid argument provided";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleIllegalArgumentException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User", "id", 123);

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleResourceNotFoundException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatusCode());
    }

    @Test
    void testHandleServiceLevelException() {
        String exceptionType = "ServiceError";
        String message = "Service operation failed";
        String description = "Database connection timeout";
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        ServiceLevelException exception = new ServiceLevelException(
                "UserService", message, "getUserById", exceptionType, description
        );
        exception.setTimestamp(timestamp);

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleServiceLevelException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(exceptionType, response.getBody().getExceptionType());
    }

    @Test
    void testHandleFileValidationException() {
        String message = "File is empty";
        FileExceptionType exceptionType = FileExceptionType.EMPTY_FILE;
        String fileName = "document.pdf";
        String details = "The uploaded file is empty";
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        FileValidationException exception = new FileValidationException(
                message, exceptionType, fileName, details, timestamp);

        ResponseEntity<FileValidationExceptionDto> response = exceptionHandler.handleFileValidationException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(message, response.getBody().getMessage());
        assertEquals(FileExceptionType.EMPTY_FILE.name(), response.getBody().getFileExceptionType());
        assertEquals(fileName, response.getBody().getFileName());
        assertEquals(details, response.getBody().getDetails());
    }

    @Test
    void testHandleUnauthorizedException() {
        String message = "Unauthorized access";
        String details = "User does not have permission";
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        UnauthorizedException exception = new UnauthorizedException(message, details);
        exception.setTimestamp(timestamp);

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleUnauthorizedException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unauthorized", response.getBody().getExceptionType());
        assertEquals(message, response.getBody().getMessage());
        assertEquals(details, response.getBody().getDescription());
    }

    @Test
    void testHandleIllegalArgumentExceptionWithNullMessage() {
        IllegalArgumentException exception = new IllegalArgumentException();

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleIllegalArgumentException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testHandleFileValidationExceptionWithUnsupportedType() {
        String message = "Unsupported file type";
        FileExceptionType exceptionType = FileExceptionType.UNSUPPORTED_TYPE;
        String fileName = "script.exe";
        String details = "Executable files are not supported";
        FileValidationException exception = new FileValidationException(
                message, exceptionType, fileName, details, new Timestamp(System.currentTimeMillis()));

        ResponseEntity<FileValidationExceptionDto> response = exceptionHandler.handleFileValidationException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(FileExceptionType.UNSUPPORTED_TYPE.name(), response.getBody().getFileExceptionType());
    }

}

