package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

/**
 * EncryptionDecryptionService
 */
public interface EncryptionDecryptionService {

	public ResponseEntity<?> decrypt(String payload);

	public ResponseEntity<?> encrypt(String payload);

}
