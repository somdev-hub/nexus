package com.nexus.pms.payload;

import java.util.Map;

import org.springframework.web.util.UriComponentsBuilder;

import lombok.Data;

@Data
public class RestPayload {
    private UriComponentsBuilder builder;

    private Map<String, String> headers;
}