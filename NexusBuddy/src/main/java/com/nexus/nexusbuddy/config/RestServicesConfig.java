package com.nexus.nexusbuddy.config;

import com.nexus.nexusbuddy.util.Logger;
import com.nexus.nexusbuddy.util.RestServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestServicesConfig {

    @Bean
    public RestServices restServices(Logger logger,
                                      @Value("${nexusbuddy.base-url:http://localhost}") String baseUrl) {
        return new RestServices(baseUrl, logger);
    }
}
