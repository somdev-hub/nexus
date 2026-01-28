package com.nexus.hr.payload;

import lombok.Data;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Data
public class RestPayload {
    private UriComponentsBuilder builder;

    private Map<String, String> headers;
}
