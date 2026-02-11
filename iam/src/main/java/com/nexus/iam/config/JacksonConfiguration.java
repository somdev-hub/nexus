package com.nexus.iam.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
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
        // Register custom serializer module
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());

        // Use JsonMapper.builder() for Jackson 3.x and add module during build
        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .addModule(module)
                .build();
    }
}








