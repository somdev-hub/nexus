package com.nexus.iam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Configuration for RestClient bean with timeout settings and robust message converters.
 * RestClient is the modern replacement for RestTemplate in Spring Framework 6.1+.
 * <p>
 * The RestTemplate is explicitly configured with StringHttpMessageConverter and
 * MappingJackson2HttpMessageConverter to handle all common response content types
 * (text/plain, application/json, etc.) without "no suitable HttpMessageConverter" errors.
 */
@Configuration
public class RestClientConfig {

    /**
     * Create a RestTemplate bean with buffering support (for timeout/retry) and
     * explicit message converters to handle text/plain, application/json, and other
     * common content types.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);

        ClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(factory);
        RestTemplate restTemplate = new RestTemplate(bufferingFactory);

        // Explicitly set message converters to ensure String and JSON are always supported.
        // StringHttpMessageConverter handles text/plain, text/html, etc.
        // MappingJackson2HttpMessageConverter handles application/json.
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setSupportedMediaTypes(List.of(
                org.springframework.http.MediaType.TEXT_PLAIN,
                org.springframework.http.MediaType.TEXT_HTML,
                org.springframework.http.MediaType.APPLICATION_JSON,
                org.springframework.http.MediaType.APPLICATION_XML,
                org.springframework.http.MediaType.ALL
        ));

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();

        restTemplate.setMessageConverters(List.of(stringConverter, jsonConverter));
        return restTemplate;
    }

    /**
     * Create RestClient bean backed by the configured RestTemplate.
     */
    @Bean
    public RestClient restClient(RestTemplate restTemplate) {
        return RestClient.create(restTemplate);
    }
}






