package com.nexus.dms.utils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.dms.dto.TokenPayloadDto;
import com.nexus.dms.exception.FileExceptionType;
import com.nexus.dms.exception.FileValidationException;

@Service
public class CommonUtils {

    @Autowired
    private WebConstants webConstants;

    private String token;

    public String generateChecksum(byte[] fileData) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(fileData);

        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public void validateFile(MultipartFile file) {
        FileValidationException ex = new FileValidationException();
        ex.setTimestamp(new Timestamp(System.currentTimeMillis()));
        if (file.isEmpty()) {
            ex.setMessage("File is empty");
            ex.setFileExceptionType(FileExceptionType.EMPTY_FILE);
            ex.setFileName("No File");
            ex.setDetails("The uploaded file is empty. Please upload a valid file.");
            ex.setTimestamp(new Timestamp(System.currentTimeMillis()));
            throw ex;
        }

        // fileNameValidation using regex
        @Nullable
        String originalFilename = file.getOriginalFilename();
        ex.setFileName(originalFilename);
        if (originalFilename != null && !originalFilename.matches("^[a-zA-Z0-9._-]+$")) {
            ex.setMessage("Invalid file name");
            ex.setFileExceptionType(FileExceptionType.INVALID_FORMAT);
            ex.setDetails(
                    "The file name contains invalid characters. Only alphanumeric characters, dots, underscores, and hyphens are allowed.");
            ex.setTimestamp(new Timestamp(System.currentTimeMillis()));
            throw ex;
        }

        // file size check

        if (file.getSize() > CommonConstants.MAX_FILE_SIZE_BYTES) {
            ex.setMessage("File size exceeded");
            ex.setFileExceptionType(FileExceptionType.SIZE_EXCEEDED);
            ex.setDetails(String.format("The uploaded file exceeds the maximum allowed size of %d MB.",
                    CommonConstants.MAX_FILE_SIZE_MB));
            throw ex;
        }

        // file type check
        String contentType = file.getContentType();
        if (!CommonConstants.ALLOWED_FILE_TYPES.contains(contentType)) {
            ex.setMessage("Unsupported file type");
            ex.setFileExceptionType(FileExceptionType.UNSUPPORTED_TYPE);
            ex.setDetails("The uploaded file type is not supported. Allowed types are PDF, JPEG, JPG, and PNG.");
            throw ex;

        }

        // password protected check for PDF files
        if (CommonConstants.PDF_VALUE.equals(contentType)) {
            try (PDDocument pdDocument = Loader.loadPDF(new RandomAccessReadBuffer(file.getBytes()))) {

                if (pdDocument.isEncrypted()) {
                    ex.setMessage("Password protected PDF");
                    ex.setFileExceptionType(FileExceptionType.UNSUPPORTED_TYPE);
                    ex.setDetails(
                            "The uploaded PDF file is password protected. Please upload an unprotected PDF file.");
                    pdDocument.close();
                    throw ex;
                }
                pdDocument.close();
            } catch (InvalidPasswordException e) {
                ex.setMessage("Password protected PDF");
                ex.setFileExceptionType(FileExceptionType.UNSUPPORTED_TYPE);
                ex.setDetails("The uploaded PDF file is password protected. Please upload an unprotected PDF file.");
                throw ex;
            } catch (IOException e) {
                ex.setMessage("Error reading PDF file");
                ex.setFileExceptionType(FileExceptionType.UNSUPPORTED_TYPE);
                ex.setDetails("An error occurred while reading the PDF file. Please ensure the file is a valid PDF.");
                throw ex;
            }
        }

    }

    public Boolean validateToken(String token) {
        String authUrl = webConstants.getVerifyTokenUrl();
        try {
            Map<String, String> body = Map.of("token", token.substring(7));
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            // extract isValid from response
            return response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null &&
                    Boolean.parseBoolean(response.getBody().get("isValid"));

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getToken() {
        if (this.token == null) {
            this.token = generateToken();
        } else if (!validateToken(this.token)) {
            this.token = generateToken();
        }
        return this.token;
    }

    public String generateToken() {
        String authUrl = webConstants.getGenerateTokenUrl();
        Map<String, String> body = new HashMap<>();
        body.put("email", "user123@nexus.com");
        body.put("password", "generic");
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            // extract token from response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().get("accessToken");
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public TokenPayloadDto decryptToken(String token) {
        String authUrl = webConstants.getDecryptTokenUrl();
        Map<String, String> body = Map.of("token", token.substring(7));
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<TokenPayloadDto> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(TokenPayloadDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
