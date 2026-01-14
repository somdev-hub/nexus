package com.nexus.dms.utils;

import com.nexus.dms.exception.FileExceptionType;
import com.nexus.dms.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonUtilsTest {

    private CommonUtils commonUtils;

    @Mock
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        commonUtils = new CommonUtils();
    }

    @Test
    void testGenerateChecksum() throws NoSuchAlgorithmException {
        byte[] testData = "test".getBytes();
        String checksum = commonUtils.generateChecksum(testData);
        assertNotNull(checksum);
        assertEquals(32, checksum.length());
        assertEquals("098f6bcd4621d373cade4e832627b4f6", checksum);
    }

    @Test
    void testGenerateChecksumEmptyData() throws NoSuchAlgorithmException {
        byte[] emptyData = new byte[0];
        String checksum = commonUtils.generateChecksum(emptyData);
        assertNotNull(checksum);
        assertEquals(32, checksum.length());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", checksum);
    }

    @Test
    void testGenerateChecksumLargeData() throws NoSuchAlgorithmException {
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        String checksum = commonUtils.generateChecksum(largeData);
        assertNotNull(checksum);
        assertEquals(32, checksum.length());
    }

    @Test
    void testValidateFileEmpty() {
        when(multipartFile.isEmpty()).thenReturn(true);
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            commonUtils.validateFile(multipartFile);
        });
        assertEquals(FileExceptionType.EMPTY_FILE, exception.getFileExceptionType());
        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void testValidateFileInvalidName() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("invalid@file#name.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1000L);
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            commonUtils.validateFile(multipartFile);
        });
        assertEquals(FileExceptionType.INVALID_FORMAT, exception.getFileExceptionType());
        assertEquals("Invalid file name", exception.getMessage());
    }

    @Test
    void testValidateFileValidName() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("valid-file_123.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1000L);
        assertDoesNotThrow(() -> {
            commonUtils.validateFile(multipartFile);
        });
    }

    @Test
    void testValidateFileSizeExceeded() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("file.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(CommonConstants.MAX_FILE_SIZE_BYTES + 1);
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            commonUtils.validateFile(multipartFile);
        });
        assertEquals(FileExceptionType.SIZE_EXCEEDED, exception.getFileExceptionType());
        assertEquals("File size exceeded", exception.getMessage());
    }

    @Test
    void testValidateFileUnsupportedType() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("file.exe");
        when(multipartFile.getContentType()).thenReturn("application/x-msdownload");
        when(multipartFile.getSize()).thenReturn(1000L);
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            commonUtils.validateFile(multipartFile);
        });
        assertEquals(FileExceptionType.UNSUPPORTED_TYPE, exception.getFileExceptionType());
        assertEquals("Unsupported file type", exception.getMessage());
    }

    @Test
    void testValidateFileNullFilename() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1000L);
        assertDoesNotThrow(() -> {
            commonUtils.validateFile(multipartFile);
        });
    }

    @Test
    void testValidateFilePdfType() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("document.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(5000L);
        assertDoesNotThrow(() -> {
            commonUtils.validateFile(multipartFile);
        });
    }

    @Test
    void testValidateFileJpegType() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("image.jpeg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(2000L);
        assertDoesNotThrow(() -> {
            commonUtils.validateFile(multipartFile);
        });
    }

    @Test
    void testValidateFilePngType() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("image.png");
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(3000L);
        assertDoesNotThrow(() -> {
            commonUtils.validateFile(multipartFile);
        });
    }

}

