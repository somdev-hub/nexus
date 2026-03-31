package com.nexus.iam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestClient bean with timeout settings
 * RestClient is the modern replacement for RestTemplate in Spring Framework 6.1+
 */
@Configuration
public class RestClientConfig {

    /**
     * Create RestClient bean with custom timeout settings for Keycloak API calls
     * Increased timeouts to handle slow Keycloak responses
     */
    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Set connection timeout to 10 seconds
        factory.setConnectTimeout(10000);
        // Set read timeout to 30 seconds (increased from 10s to handle slow JWKS endpoint)
        factory.setReadTimeout(30000);
        
        ClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(factory);
        RestTemplate restTemplate = new RestTemplate(bufferingFactory);
        return RestClient.create(restTemplate);
    }
}






