package com.nexus.cms.payload;

import lombok.Data;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Data
public class RestPayload {
    private UriComponentsBuilder builder;

    private Map<String, String> headers;
}
