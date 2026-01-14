package com.nexus.dms.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class FileValidationExceptionTest {

    @Test
    void testConstructorNoArgs() {
        // Act
        FileValidationException exception = new FileValidationException();

        // Assert
        assertNotNull(exception);
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertNull(exception.getMessage());
    }

    @Test
    void testConstructorAllArgs() {
        // Arrange
        String message = "File validation failed";
        FileExceptionType exceptionType = FileExceptionType.EMPTY_FILE;
        String fileName = "test.pdf";
        String details = "File is empty";
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        // Act
        FileValidationException exception = new FileValidationException(
                message, exceptionType, fileName, details, timestamp);

        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(exceptionType, exception.getFileExceptionType());
        assertEquals(fileName, exception.getFileName());
        assertEquals(details, exception.getDetails());
        assertEquals(timestamp, exception.getTimestamp());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        FileValidationException exception = new FileValidationException();
        String message = "Invalid file";
        FileExceptionType type = FileExceptionType.UNSUPPORTED_TYPE;
        String fileName = "document.exe";
        String details = "Executable files are not allowed";
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        // Act
        exception.setMessage(message);
        exception.setFileExceptionType(type);
        exception.setFileName(fileName);
        exception.setDetails(details);
        exception.setTimestamp(timestamp);

        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(type, exception.getFileExceptionType());
        assertEquals(fileName, exception.getFileName());
        assertEquals(details, exception.getDetails());
        assertEquals(timestamp, exception.getTimestamp());
    }

    @Test
    void testStatusAlwaysBadRequest() {
        // Arrange
        FileValidationException exception = new FileValidationException();

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testIsRuntimeException() {
        // Assert
        assertTrue(FileValidationException.class.isAssignableFrom(RuntimeException.class) ||
                   new FileValidationException() instanceof RuntimeException);
    }

    @Test
    void testExceptionWithOnlyMessage() {
        // Arrange
        String message = "File validation error";

        // Act
        FileValidationException exception = new FileValidationException();
        exception.setMessage(message);

        // Assert
        assertEquals(message, exception.getMessage());
        assertNull(exception.getFileExceptionType());
        assertNull(exception.getFileName());
    }

    @Test
    void testExceptionWithFileExceptionType() {
        // Arrange
        FileExceptionType type = FileExceptionType.SIZE_EXCEEDED;

        // Act
        FileValidationException exception = new FileValidationException();
        exception.setFileExceptionType(type);

        // Assert
        assertEquals(type, exception.getFileExceptionType());
    }

}

