package com.nexus.nexusencryption;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public final class NexusEncryption {

    /**
     * CryptoUtil
     * ----------
     * Public API: encrypt(String, String) and decrypt(String, String) only.
     * <p>
     * Internals:
     * - The passphrase you supply is run through SHA-256 to derive a 256-bit AES key
     * (so SHA-256 is doing key derivation, which is what it's actually good for).
     * - Actual reversible encryption is AES-256 in GCM mode, which gives you
     * confidentiality AND tamper detection (integrity) — this is the current
     * industry-standard authenticated cipher, and it is what genuinely
     * "encrypts" and "decrypts" your data, unlike a hash.
     * - Each call generates a fresh random 12-byte IV/nonce. The IV is prepended
     * to the ciphertext and the whole thing is Base64-encoded, so the output
     * string is self-contained: decrypt() just needs that string + the same key.
     * <p>
     * Usage:
     * String cipherText = CryptoUtil.encrypt("Hello World", "mySecretKey");
     * String plainText  = CryptoUtil.decrypt(cipherText, "mySecretKey");
     */

    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // bytes, recommended for GCM
    private static final int GCM_TAG_LENGTH = 128;  // bits

    // Prevent instantiation — everything is exposed as static methods.
    private NexusEncryption() {
    }

    /**
     * Encrypts plainText using a key derived (via SHA-256) from secretKey.
     *
     * @param plainText the text to encrypt
     * @return Base64-encoded string containing IV + ciphertext + auth tag
     */
    public static String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = deriveKey(NexusSecret.NEXUS_SECRET_KEY_FOR_ENCRYPTION_AND_DECRYPTION);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Prepend IV so decrypt() can pull it back out later.
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a string previously produced by encrypt(), using the same secretKey.
     *
     * @param cipherTextBase64 the Base64 string returned by encrypt()
     * @return the original plaintext
     */
    public static String decrypt(String cipherTextBase64) {
        try {
            SecretKeySpec keySpec = deriveKey(NexusSecret.NEXUS_SECRET_KEY_FOR_ENCRYPTION_AND_DECRYPTION);

            byte[] combined = Base64.getDecoder().decode(cipherTextBase64);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed (wrong key, or data was tampered with/corrupted)", e);
        }
    }

    /**
     * Derives a 256-bit AES key from a passphrase using SHA-256.
     * Note: for production systems with low-entropy human passwords, prefer
     * PBKDF2/Argon2 (salted + iterated) over a bare SHA-256 hash — see the
     * deriveKeyPBKDF2 alternative below if you need that.
     */
    private static SecretKeySpec deriveKey(String secretKey) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(secretKey.getBytes(StandardCharsets.UTF_8)); // always 32 bytes = 256 bits
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Optional stronger key derivation (salted + iterated) if you ever need
     * to harden this against brute-force on weak passphrases. Not wired into
     * encrypt/decrypt by default to keep the public API's behavior simple
     * and stateless (no salt to manage/store), but kept here for reference.
     */
    private static SecretKeySpec deriveKeyPBKDF2(String secretKey, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt, 100_000, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    static void main() {
        IO.println("NexusEncryption Test");
    }
}
