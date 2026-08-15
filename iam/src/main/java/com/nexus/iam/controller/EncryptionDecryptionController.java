package com.nexus.iam.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.service.EncryptionDecryptionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/iam/encryption-decryption")
@RequiredArgsConstructor
public class EncryptionDecryptionController {

	private final EncryptionDecryptionService encryptionDecryptionService;

	@PostMapping("/encrypt")
	public ResponseEntity<?> encrypt(@RequestBody String payload) {
		return encryptionDecryptionService.encrypt(payload);
	}
	

	@PostMapping("/decrypt")
	public ResponseEntity<?> decrypt(@RequestBody String payload) {
		return encryptionDecryptionService.decrypt(payload);
	}
	
	
}
