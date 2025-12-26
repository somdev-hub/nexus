package com.nexus.core.payload;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class TokenPayloadDto {

    private boolean isValid;
    private List<Map<String, String>> roles;
    private long expiration;
    private long issuedAt;
    private String type;
    private long userId;
    private String username;
}
