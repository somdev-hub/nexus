package com.nexus.dms.service.impl;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.dms.dto.UploaderResponse;
import com.nexus.dms.service.UploaderService;
import com.nexus.dms.utils.WebConstants;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class UploaderServiceImpl implements UploaderService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private WebConstants webConstants;

    @Override
    public ResponseEntity<UploaderResponse> uploadFile(MultipartFile file, String fileName, String bucketName)
            throws IOException {
        byte[] fileBytes = file.getBytes();
        int maxRetries = 3;
        int retryCount = 0;
        boolean uploadSuccessful = false;

        while (retryCount < maxRetries && !uploadSuccessful) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
                uploadSuccessful = true;
            } catch (SdkClientException | S3Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new IOException("File upload failed after retries", e);
                }
                try {
                    Thread.sleep(1000); // Wait for 1 second before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("File upload interrupted", ie);
                }
            }
        }

        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
        String cid = headObjectResponse.metadata().get("cid");

        UploaderResponse response = new UploaderResponse(fileName, cid, getFileUrl(cid), bucketName);
        return ResponseEntity.ok(response);
    }

    private String getFileUrl(String cid) {
        return String.format("%s/%s", webConstants.getS3Endpoint(), cid);
    }

    // https://ipfs.filebase.io/ipfs/QmVrQV3SjhMH3kScV2Sm2xxfXxpFnKT7sNr85nbkawN365
    @Override
    public Boolean deleteFile(String dmsId, String bucketName) {
        // Extract the CID from the URL
        String cid = dmsId.substring(dmsId.lastIndexOf("/") + 1);
        System.out.println(dmsId);
        try {
            // Check if the object exists in the S3 bucket
            HeadObjectResponse headObject = s3Client.headObject(builder -> builder.bucket(bucketName).key(cid));
            System.out.println("Object exists: " + headObject);

            // Use the CID as the key to delete the object from the S3 bucket
            DeleteObjectResponse deleteObject = s3Client
                    .deleteObject(builder -> builder.bucket(bucketName).key(cid));
            System.out.println(deleteObject);
            return true;
        } catch (NoSuchKeyException e) {
            System.out.println("Object does not exist.");
            return false;
        } catch (S3Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

}
