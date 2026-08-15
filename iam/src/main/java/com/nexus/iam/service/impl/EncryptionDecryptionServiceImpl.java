package com.nexus.iam.service.impl;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.iam.service.EncryptionDecryptionService;
import com.nexus.nexusencryption.NexusEncryption;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EncryptionDecryptionServiceImpl implements EncryptionDecryptionService {
	@Override
	public ResponseEntity<?> decrypt(String payload) {
		if (payload == null || payload.isEmpty()) {
			return ResponseEntity.badRequest().body("Payload cannot be null or empty");
		}
		try {
			JSONObject jsonObject = new JSONObject(payload); // Validate if the payload is a valid JSON
			String payloadString = jsonObject.optString("payload");
			if (payloadString == null || payloadString.isEmpty()) {
				return ResponseEntity.badRequest().body("Payload must contain a 'payload' field");
			}
			String decryptedPayload = NexusEncryption.decrypt(payloadString);
			return ResponseEntity.ok().body(decryptedPayload);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error occurred while decrypting payload");
		}
	}

	@Override
	public ResponseEntity<?> encrypt(String payload) {
		if (payload == null || payload.isEmpty()) {
			return ResponseEntity.badRequest().body("Payload cannot be null or empty");
		}
		try {
			JSONObject jsonObject = new JSONObject(payload); // Validate if the payload is a valid JSON
			String payloadString = jsonObject.optString("payload");
			if (payloadString == null || payloadString.isEmpty()) {
				return ResponseEntity.badRequest().body("Payload must contain a 'payload' field");
			}
			String encryptedPayload = NexusEncryption.encrypt(payloadString);
			return ResponseEntity.ok().body(encryptedPayload);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error occurred while encrypting payload");
		}
	}

}
