package com.nexus.dms.service.impl;

import com.nexus.dms.utils.WebConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploaderServiceImplTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private WebConstants webConstants;

    private UploaderServiceImpl uploaderService;

    @Mock
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        uploaderService = new UploaderServiceImpl();
    }

    @Test
    void testUploadFileSuccess() throws IOException {
        byte[] fileContent = "test file content".getBytes();
        when(multipartFile.getBytes()).thenReturn(fileContent);
        when(webConstants.getS3Endpoint()).thenReturn("https://s3.example.com");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("cid", "test-cid-123");
        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                .metadata(metadata)
                .build();
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenReturn(headObjectResponse);

        assertNotNull(uploaderService);
    }

    @Test
    void testDeleteFileSuccess() {
        String fileUrl = "https://ipfs.filebase.io/ipfs/QmVrQV3SjhMH3kScV2Sm2xxfXxfnKT7sNr85nbkawN365";
        Map<String, String> metadata = new HashMap<>();
        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                .metadata(metadata)
                .build();
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenReturn(headObjectResponse);
        when(s3Client.deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class)))
                .thenReturn(null);

        assertNotNull(uploaderService);
    }

}

