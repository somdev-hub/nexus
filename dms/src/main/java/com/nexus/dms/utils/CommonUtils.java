package com.nexus.dms.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nexus.dms.dto.TokenPayloadDto;
import com.nexus.dms.exception.FileExceptionType;
import com.nexus.dms.exception.FileValidationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Service
public class CommonUtils {

    private final WebConstants webConstants;

    private String token;

    public CommonUtils(WebConstants webConstants) {
        this.webConstants = webConstants;
    }

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
        if (file.isEmpty()) {
            throw new FileValidationException(
                    "File is empty",
                    HttpStatus.BAD_REQUEST,
                    FileExceptionType.EMPTY_FILE,
                    "No File",
                    "The uploaded file is empty. Please upload a valid file.",
                    new Timestamp(System.currentTimeMillis())
            );
        }

        // fileNameValidation using regex
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new FileValidationException(
                    "Invalid file name",
                    HttpStatus.BAD_REQUEST,
                    FileExceptionType.INVALID_FORMAT,
                    originalFilename,
                    "The file name contains invalid characters. Only alphanumeric characters, dots, underscores, and hyphens are allowed.",
                    new Timestamp(System.currentTimeMillis())
            );
        }

        // file size check

        if (file.getSize() > CommonConstants.MAX_FILE_SIZE_BYTES) {
            throw new FileValidationException(
                    "File size exceeded",
                    HttpStatus.BAD_REQUEST,
                    FileExceptionType.SIZE_EXCEEDED,
                    originalFilename,
                    String.format("The uploaded file exceeds the maximum allowed size of %d MB.",
                            CommonConstants.MAX_FILE_SIZE_MB),
                    new Timestamp(System.currentTimeMillis())
            );
        }

        // file type check
        String contentType = file.getContentType();
        if (!CommonConstants.ALLOWED_FILE_TYPES.contains(contentType)) {
            throw new FileValidationException(
                    "Unsupported file type",
                    HttpStatus.BAD_REQUEST,
                    FileExceptionType.UNSUPPORTED_TYPE,
                    originalFilename,
                    "The uploaded file type is not supported. Allowed types are PDF, JPEG, JPG, and PNG.",
                    new Timestamp(System.currentTimeMillis())
            );

        }

        // password protected check for PDF files
        if (CommonConstants.PDF_VALUE.equals(contentType)) {
            try (PDDocument pdDocument = Loader.loadPDF(new RandomAccessReadBuffer(file.getBytes()))) {

                if (pdDocument.isEncrypted()) {
                    throw new FileValidationException(
                            "Password protected PDF",
                            HttpStatus.BAD_REQUEST,
                            FileExceptionType.UNSUPPORTED_TYPE,
                            originalFilename,
                            "The uploaded PDF file is password protected. Please upload an unprotected PDF file.",
                            new Timestamp(System.currentTimeMillis())
                    );
                }

            } catch (InvalidPasswordException e) {
                throw new FileValidationException(
                        "Password protected PDF",
                        HttpStatus.BAD_REQUEST,
                        FileExceptionType.UNSUPPORTED_TYPE,
                        originalFilename,
                        e.getMessage(),
                        new Timestamp(System.currentTimeMillis())
                );
            } catch (IOException e) {
                throw new FileValidationException(
                        "Error reading PDF file",
                        HttpStatus.BAD_REQUEST,
                        FileExceptionType.UNSUPPORTED_TYPE,
                        originalFilename,
                        e.getMessage(),
                        new Timestamp(System.currentTimeMillis())
                );
            }
        }

    }

    public boolean validateToken(String token) {
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
            Map<String, String> responseBody = response.getBody();
            return !response.getStatusCode().is2xxSuccessful() ||
                    !ObjectUtils.isEmpty(responseBody) ||
                    !Boolean.parseBoolean(responseBody.get("isValid"));

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public String getToken() {
        if (this.token == null || !validateToken(this.token)) {
            this.token = generateToken();
        }
        return this.token;
    }

    public String generateToken() {
        String authUrl = webConstants.getGenerateTokenUrl();
        Map<String, String> body = new HashMap<>();
        body.put("email", webConstants.getGenericUserId());
        body.put("password", webConstants.getGenericPassword());
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {
                    });
            // extract token from response
            Map<String, String> responseBody = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && responseBody != null && responseBody.containsKey("accessToken")) {
                return "Bearer " + responseBody.get("accessToken");
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

    public String jsonValidator(String jsonString) {
        if (ObjectUtils.isEmpty(jsonString)) {
            return "{}";
        }
        JsonNode jsonNode = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            jsonNode = objectMapper.readTree(jsonString);
        }
        catch (JacksonException _){
            jsonNode = objectMapper.createObjectNode().put("message", jsonString);
        }
        return objectMapper.writeValueAsString(jsonNode);
    }


}
