package com.nexus.cms.payload;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TokenPayloadDto {

    private boolean isValid;
    private List<Map<String, String>> roles;
    private long expiration;
    private long issuedAt;
    private String type;
    private Long userId;
    private Long orgId;
    private String username;
    private String email;
}
