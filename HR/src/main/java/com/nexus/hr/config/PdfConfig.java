package com.nexus.hr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties for PDF generation
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.pdf")
public class PdfConfig {

    private String organizationName = "Organization";

    private String organizationAddress = "Organization Address";

    private String hrContactEmail = "hr@organization.com";

    private String hrContactPhone = "+1-XXX-XXX-XXXX";

    private String outputDirectory = "./generated_documents";

    private boolean storeLocally = false;
}
