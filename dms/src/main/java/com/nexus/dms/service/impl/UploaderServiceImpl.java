package com.nexus.dms.service.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.dms.dto.UploaderResponse;
import com.nexus.dms.service.UploaderService;
import com.nexus.dms.utils.CommonConstants;
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

    private final S3Client s3Client;

    private final WebConstants webConstants;

    public UploaderServiceImpl(S3Client s3Client, WebConstants webConstants) {
        this.s3Client = s3Client;
        this.webConstants = webConstants;
    }

    @Override
    public ResponseEntity<UploaderResponse> uploadFile(MultipartFile file, String fileName, String folderPrefix)
            throws IOException {
        byte[] fileBytes = file.getBytes();
        int maxRetries = 3;
        int retryCount = 0;
        boolean uploadSuccessful = false;
        // Construct the S3 key with folder prefix
        String s3Key = folderPrefix != null ? folderPrefix + "/" + fileName : fileName;

        while (!uploadSuccessful) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(CommonConstants.MAIN_BUCKET)
                        .key(s3Key)
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
                .bucket(CommonConstants.MAIN_BUCKET)
                .key(s3Key)
                .build();

        HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
        String cid = headObjectResponse.metadata().get("cid");

        UploaderResponse response = new UploaderResponse(fileName, cid, getFileUrl(cid),
                CommonConstants.MAIN_BUCKET + "/" + folderPrefix);
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

        try {
            // Check if the object exists in the S3 bucket
            HeadObjectResponse headObject = s3Client
                    .headObject(builder -> builder.bucket(CommonConstants.MAIN_BUCKET).key(cid));

            // Use the CID as the key to delete the object from the S3 bucket
            if (!ObjectUtils.isEmpty(headObject)) {
                DeleteObjectResponse deleteObject = s3Client
                        .deleteObject(builder -> builder.bucket(CommonConstants.MAIN_BUCKET).key(cid));

                return !ObjectUtils.isEmpty(deleteObject);

            }
            return false;
        } catch (S3Exception _) {
            return false;
        }
    }

}
