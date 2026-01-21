package com.nexus.hr.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for encrypting and decrypting sensitive data using AES-256-GCM
 */
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${encryption.secret.key:MySecretEncryptionKey32BytesLong!}")
    private String secretKeyString;

    /**
     * Encrypts the given plaintext using AES-256-GCM
     *
     * @param plaintext The text to encrypt
     * @return Base64 encoded encrypted text with IV prepended
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            // Generate IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Create cipher instance
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // Initialize cipher with key
            SecretKey key = getSecretKey();
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // Encrypt the plaintext
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            // Encode to Base64
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    /**
     * Decrypts the given ciphertext using AES-256-GCM
     *
     * @param ciphertext Base64 encoded encrypted text with IV prepended
     * @return Decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }

        try {
            // Decode from Base64
            byte[] decodedBytes = Base64.getDecoder().decode(ciphertext);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // Create cipher instance
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // Initialize cipher with key
            SecretKey key = getSecretKey();
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // Decrypt the ciphertext
            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }

    /**
     * Generates the SecretKey from the configured secret key string
     *
     * @return SecretKey for AES encryption
     */
    private SecretKey getSecretKey() {
        // Ensure key is exactly 32 bytes for AES-256
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes32 = new byte[32];
        System.arraycopy(keyBytes, 0, keyBytes32, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(keyBytes32, "AES");
    }
}
