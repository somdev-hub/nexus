package com.nexus.core.utils;

import org.springframework.beans.factory.annotation.Value;

import lombok.Data;

@Data
public class CommonConstants {

    @Value("${verify.token.url}")
    public String verifyTokenUrl;

    @Value("${generate.token.url}")
    public String generateTokenUrl;
}
