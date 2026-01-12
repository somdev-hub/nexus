package com.nexus.dms.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class WebConstants {

    @Value("${fetch.user.org.details.url}")
    private String fetchUserOrgDetailsUrl;

    @Value("${verify.token.url}")
    public String verifyTokenUrl;

    @Value("${generate.token.url}")
    public String generateTokenUrl;

    @Value("${decrypt.token.url}")
    public String decryptTokenUrl;

}
