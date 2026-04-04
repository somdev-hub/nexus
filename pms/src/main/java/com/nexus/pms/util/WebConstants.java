package com.nexus.pms.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class WebConstants {
    @Value("${dms.upload.org.documents}")
    private String dmsOrgDocumentUploadUrl;

    @Value("${fetch.user.org.details.url}")
    private String fetchUserOrgDetailsUrl;

    @Value("${verify.token.url}")
    public String verifyTokenUrl;

    @Value("${generate.token.url}")
    public String generateTokenUrl;

    @Value("${decrypt.token.url}")
    public String decryptTokenUrl;

    @Value("${generic.user.id}")
    public String genericUserId;

    @Value("${generic.password}")
    public String genericPassword;
}
