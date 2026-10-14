package com.nexus.pms.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Opaque payment-reference crypto.
 * AES-256-GCM with SHA-256 derived key, random 12-byte IV prepended,
 * Base64-URL encoded so it is safe as a ?ref= query param.
 * Tamper-evident: forged refs fail decrypt and are rejected.
 */
@Component
@Slf4j
public class SuiteRefCrypto {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    @Value("${suite.payments.ref-secret:nexus-suite-pay-dev-secret-change-me}")
    private String secret;

    public String encrypt(String plain) {
        try {
            SecretKeySpec key = derive();
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ct, 0, combined, iv.length, ct.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("ref encrypt failed", e);
        }
    }

    public String decrypt(String token) {
        try {
            SecretKeySpec key = derive();
            byte[] combined = Base64.getUrlDecoder().decode(token);
            if (combined.length <= IV_LEN)
                throw new IllegalArgumentException("bad ref");
            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[combined.length - IV_LEN];
            System.arraycopy(combined, 0, iv, 0, IV_LEN);
            System.arraycopy(combined, IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("ref decrypt failed (tampered/expired key?)");
            throw new IllegalArgumentException("Invalid payment reference");
        }
    }

    private SecretKeySpec derive() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] k = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(k, "AES");
    }
}
