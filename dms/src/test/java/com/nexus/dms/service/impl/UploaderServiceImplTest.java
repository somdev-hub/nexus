package com.nexus.dms.service.impl;

import com.nexus.dms.dto.UploaderResponse;
import com.nexus.dms.utils.CommonConstants;
import com.nexus.dms.utils.WebConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploaderServiceImplTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private WebConstants webConstants;

    @InjectMocks
    private UploaderServiceImpl uploaderService;

    @Mock
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }

    @Test
    void testUploadFileSuccess() throws IOException {
        // Arrange
        byte[] fileContent = "test file content".getBytes();
        String fileName = "test.txt";
        String folderPrefix = "uploads";
        String cid = "test-cid-123";
        String s3Endpoint = "https://s3.example.com";

        when(multipartFile.getBytes()).thenReturn(fileContent);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("cid", cid);
        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                .metadata(metadata)
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headObjectResponse);
        when(webConstants.getS3Endpoint()).thenReturn(s3Endpoint);

        // Act
        ResponseEntity<UploaderResponse> response = uploaderService.uploadFile(multipartFile, fileName, folderPrefix);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(fileName, response.getBody().getFileName());
        assertEquals(cid, response.getBody().getDmsId());
        assertEquals(s3Endpoint + "/" + cid, response.getBody().getUrl());
    }

    @Test
    void testDeleteFileSuccess() {
        // Arrange
        String dmsId = "https://ipfs.filebase.io/ipfs/QmVrQV3SjhMH3kScV2Sm2xxfXxfnKT7sNr85nbkawN365";

        Map<String, String> metadata = new HashMap<>();
        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                .metadata(metadata)
                .build();
        when(s3Client.headObject(any(Consumer.class)))
                .thenReturn(headObjectResponse);
        when(s3Client.deleteObject(any(Consumer.class)))
                .thenReturn(null);

        // Act
        Boolean result = uploaderService.deleteFile(dmsId, CommonConstants.MAIN_BUCKET);

        // Assert
        assertTrue(result);
    }

}
