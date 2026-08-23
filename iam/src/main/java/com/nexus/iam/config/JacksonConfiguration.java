package com.nexus.iam.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nexus.iam.dto.response.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * Jackson configuration for custom serializers
 * Registers custom serializers for LocalDateTime and other types
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper customObjectMapper() {
        // Create ObjectMapper for Jackson 2.x
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Register custom serializer module
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }
}
