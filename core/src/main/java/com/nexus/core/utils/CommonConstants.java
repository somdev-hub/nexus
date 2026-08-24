package com.nexus.core.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class CommonConstants {

	@Value("${verify.token.url}")
	public String verifyTokenUrl;

	@Value("${generate.token.url}")
	public String generateTokenUrl;

	@Value("${decrypt.token.url}")
	public String decryptTokenUrl;

	// IAM Service URLs
	@Value("${iam.service.url:http://localhost:8081}")
	public String iamServiceUrl;

	@Value("${iam.organization.url}")
	public String iamOrganizationUrl;

	@Value("${iam.user.url}")
	public String iamUserUrl;

	// Common HTTP Constants
	public static final String AUTHORIZATION = "Authorization";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String APPLICATION_JSON = "application/json";
	public static final String X_ORGANIZATION_ID = "X-Organization-ID";
	public static final String BEARER_PREFIX = "Bearer ";
}
