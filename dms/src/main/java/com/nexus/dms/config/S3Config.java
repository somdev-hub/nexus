package com.nexus.dms.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3Config {

        @Value("${s3.accessKeyId}")
        private String accessKeyId;

        @Value("${s3.secretAccessKey}")
        private String secretAccessKey;

        @Value("${s3.region}")
        private String region;

        @Value("${s3.endpoint}")
        private String endpoint;

        @Bean
        public S3Client s3Client() {
                return S3Client.builder()
                                .region(Region.of(region))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                                .endpointOverride(URI.create(endpoint))
                                .serviceConfiguration(S3Configuration.builder()
                                                .pathStyleAccessEnabled(true)
                                                .build())
                                .build();
        }
}