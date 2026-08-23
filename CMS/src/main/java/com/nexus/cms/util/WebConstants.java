package com.nexus.cms.util;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class WebConstants {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${spring.mail.from:noreply@nexushr.com}")
    private String defaultFromEmail;

    @Value("${spring.mail.max-recipients:100}")
    private Integer maxRecipients;

    @Value("${verify.token.url}")
    public String verifyTokenUrl;

    @Value("${generate.token.url}")
    public String generateTokenUrl;

    @Value("${decrypt.token.url}")
    public String decryptTokenUrl;

    @Value("${generic.user.id}")
    public String genericUserId;

    @Value("${generic.password}")
    public String genericPassword;

    @Value("${dms.upload.org.documents}")
    private String dmsOrgDocumentUploadUrl;

    @Value("${dms.upload.individual.documents}")
    private String dmsIndividualDocumentUploadUrl;

}
