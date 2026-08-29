package com.nexus.core.utils;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class WebConstants {

	// IAM Service URLs
	@Value("${iam.service.url:http://localhost:8081}")
	private String iamServiceUrl;

	@Value("${iam.organization.url:/iam/organizations}")
	private String iamOrganizationUrl;

	@Value("${iam.user.url:/iam/users}")
	private String iamUserUrl;

	@Value("${iam.auth.url:/iam/auth}")
	private String iamAuthUrl;

	// Core Service URLs
	@Value("${core.service.url:http://localhost:8082}")
	private String coreServiceUrl;

	@Value("${core.products.url:/core/products}")
	private String coreProductsUrl;

	@Value("${core.materials.url:/core/materials}")
	private String coreMaterialsUrl;

	@Value("${core.warehouses.url:/core/warehouses}")
	private String coreWarehousesUrl;

	@Value("${core.orders.url:/core/orders}")
	private String coreOrdersUrl;

	@Value("${core.partnerships.url:/core/partnerships}")
	private String corePartnershipsUrl;

	// DMS Service URLs
	@Value("${dms.service.url:http://localhost:8083}")
	private String dmsServiceUrl;

	@Value("${individual.dms.url:/dms/upload/individual}")
	private String individualFileUploadUrl;

	@Value("${org.dms.url:/dms/upload/org}")
	private String orgFileUploadUrl;

	@Value("${common.dms.url:/dms/upload/common}")
	private String commonDmsUrl;

	// HR Service URLs
	@Value("${hr.service.url:http://localhost:8084}")
	private String hrServiceUrl;

	@Value("${hr.init.url}")
	private String hrInitUrl;

	@Value("${hr.employee.directory.url}")
	private String employeeDirectoryUrl;

	@Value("${hr.employee.details.url}")
	private String employeeDetailsUrl;

	// CMS Service URLs
	@Value("${cms.service.url:http://localhost:8085}")
	private String cmsServiceUrl;

	@Value("${cms.conversation.url}")
	private String cmsConversationUrl;

	@Value("${cms.message.url}")
	private String cmsMessageUrl;

	// PMS Service URLs
	@Value("${pms.service.url:http://localhost:8086}")
	private String pmsServiceUrl;

	// Keycloak Configuration
	@Value("${keycloak.server-url:http://localhost:9090}")
	private String keycloakServerUrl;

	@Value("${keycloak.realm:nexus}")
	private String keycloakRealm;

	@Value("${keycloak.client-id:core-client}")
	private String keycloakClientId;

	@Value("${keycloak.client-secret:}")
	private String keycloakClientSecret;

	// Generic user for service-to-service calls
	@Value("${generic.user.id}")
	private String genericUserId;

	@Value("${generic.password}")
	private String genericPassword;

	// Keycloak helper methods
	public String getKeycloakAdminUrl() {
		return keycloakServerUrl + "/admin/realms/" + keycloakRealm;
	}

	public String getKeycloakTokenUrl() {
		return keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
	}

	public String getKeycloakJwksUrl() {
		return keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/certs";
	}

	public String getKeycloakUserInfoUrl() {
		return keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/userinfo";
	}

	public String getKeycloakAuthorizationUrl() {
		return keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/auth";
	}

	public String getKeycloakLogoutUrl() {
		return keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/logout";
	}

	// DMS URL helper methods
	public String getIndividualFileUploadUrl() {
		return dmsServiceUrl + individualFileUploadUrl;
	}

	public String getOrgFileUploadUrl() {
		return dmsServiceUrl + orgFileUploadUrl;
	}

	public String getCommonFileUploadUrl() {
		return dmsServiceUrl + commonDmsUrl;
	}
}