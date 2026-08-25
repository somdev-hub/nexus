package com.nexus.iam.utils;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class WebConstants {

	// ...existing DMS and HR properties...
	@Value("${dms.service.url}")
	private String dmsServiceUrl;

	@Value("${individual.dms.url}")
	private String individualFileUploadUrl;

	@Value("${org.dms.url}")
	private String orgFileUploadUrl;

	@Value("${common.dms.url}")
	private String commonDmsUrl;

	@Value("${generic.user.id}")
	private String genericUserId;

	@Value("${generic.password}")
	private String genericPassword;

	@Value("${hr.init.url}")
	private String hrInitUrl;

	@Value("${hr.employee.paycheck.url}")
	private String employeePaycheckUrl;

	@Value("${hr.employee.paycheck.get.url}")
	private String employeePaycheckGetUrl;

	@Value("${hr.employee.onnoticeperiod}")
	private String employeeOnNoticePeriodUrl;

	@Value("${hr.employee.directory.url}")
	private String employeeDirectoryUrl;

	@Value("${hr.employee.details.url}")
	private String employeeDetailsUrl;

	// ============================================
	// KEYCLOAK CONFIGURATION PROPERTIES
	// ============================================

	@Value("${keycloak.server-url:http://localhost:9090}")
	private String keycloakServerUrl;

	@Value("${keycloak.realm:nexus}")
	private String keycloakRealm;

	@Value("${keycloak.client-id:iam-client}")
	private String keycloakClientId;

	@Value("${keycloak.client-secret:}")
	private String keycloakClientSecret;

	@Value("${keycloak.admin-client-id:admin-cli}")
	private String keycloakAdminClientId;

	@Value("${keycloak.admin-client-secret:}")
	private String keycloakAdminClientSecret;

	@Value("${keycloak.admin-grant-type:client_credentials}")
	private String keycloakAdminGrantType;

	@Value("${keycloak.role-client-id:iam-client}")
	private String keycloakRoleClientId;

	// OAuth2/OIDC Properties (Phase 2)
	@Value("${keycloak.oauth2.client-id:iam-client}")
	private String keycloakOAuth2ClientId;

	@Value("${keycloak.oauth2.client-secret:}")
	private String keycloakOAuth2ClientSecret;

	@Value("${keycloak.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
	private String keycloakOAuth2RedirectUri;

	@Value("${keycloak.oauth2.enabled:true}")
	private boolean keycloakOAuth2Enabled;

	@Value("${hr.employee.attendance.url}")
	private String employeeAttendanceUrl;

	@Value("${hr.employee.attendance.toggle.url}")
	private String toggleAttendanceUrl;

	@Value("${hr.initiate.payroll.url}")
	private String initiatePayrollUrl;
	@Value("${payroll.employees.url}")
	private String payrollEmployeesUrl;

	@Value("${hr.employee.this.month.attendance.url}")
	private String employeeThisMonthAttendanceUrl;

	@Value("${hr.processed.payrolls.url}")
	private String processedPayrollsUrl;

	@Value("${payroll.graphs.url}")
	private String payrollGraphsUrl;

	@Value("${payroll.insights.url}")
	private String payrollInsightsUrl;

	@Value("${create.hr.request.url}")
	private String createHrRequestUrl;

	@Value("${many.hr.request.url}")
	private String manyHrRequestsUrl;

	@Value("${take.action.on.hr.request.url}")
	private String takeActionOnHrRequestUrl;

	@Value("${hr.request.insights.url}")
	private String hrRequestInsightsUrl;

	@Value("${hr.closed.requests.url}")
	private String closedHrRequestsUrl;

	@Value("${employee.avg.strength.url}")
	private String employeeAveStrengthUrl;

	@Value("${leave.type.distribution.url}")
	private String leaveTypeDistributionUrl;

	@Value("${employee.checkin.checkout.url}")
	private String employeeCheckInCheckOutUrl;

	@Value("${employee.break.start.end.url}")
	private String employeeBreakStartEndUrl;

	@Value("${payroll.yearly.url}")
	private String payrollYearlyUrl;

	@Value("${payroll.role.wise.url}")
	private String payrollRoleWiseUrl;

	@Value("${overtime.anomaly.url}}")
	private String overtimeAnomalyUrl;

	@Value("${leaves.role.wise.url}")
	private String leavesRoleWiseUrl;

	@Value("${leaves.department.wise.url}")
	private String leavesDepartmentWiseUrl;

	@Value("${time.management.quick.update.url}")
	private String timeManagementQuickUpdateUrl;

	@Value("${hero.analytics.url}")
	private String heroAnalyticsUrl;

	// Weekly analytics URLs (last 7 days)
	@Value("${weekly.employee.strength.url}")
	private String weeklyEmployeeStrengthUrl;

	@Value("${weekly.working.hours.url}")
	private String weeklyWorkingHoursUrl;

	@Value("${weekly.checkin.checkout.url}")
	private String weeklyCheckInCheckOutUrl;

	@Value("${today.applied.hr.requests.url}")
	private String todayAppliedHrRequestsUrl;

	@Value("${hr.recuitment.url}")
	private String hrRecruitmentUrl;

	@Value("${hr.applicant.url}")
	private String applicantUrl;

	@Value("${cms.conversation.url}")
	private String cmsConversationUrl;

	@Value("${cms.message.url}")
	private String cmsMessageUrl;

	// ============================================
	// NEW CHAT SERVICE (v2) PROPERTIES
	// ============================================
	@Value("${cms.new.chat.conversation.url}")
	private String cmsNewChatConversationUrl;

	@Value("${cms.new.chat.conversations.url}")
	private String cmsNewChatConversationsUrl;

	@Value("${cms.new.chat.conversation.details.url}")
	private String cmsNewChatConversationDetailsUrl;

	@Value("${cms.new.chat.view.url}")
	private String cmsNewChatViewUrl;

	@Value("${cms.new.chat.message.url}")
	private String cmsNewChatMessageUrl;

	@Value("${cms.new.chat.multimedia.url}")
	private String cmsNewChatMultimediaUrl;

	@Value("${cms.new.chat.messages.url}")
	private String cmsNewChatMessagesUrl;

	@Value("${cms.chat.presence.batch.url}")
	private String cmsChatPresenceBatchUrl;

	@Value("${event.onboarding.template.url}")
	private String eventOnboardingTemplateUrl;

	@Value("${event.onboarding.template.params.url}")
	private String eventOnboardingTemplateParamsUrl;

	@Value("${event.onboarding.trigger.mail.url}")
	private String triggerEventOnboardingMailUrl;

	@Value("${event.onboarding.event.hits.url}")
	private String eventOnboardingHitsUrl;

	@Value("${event.onboarding.event.status.breakdown.url}")
	private String eventStatusBreakdownUrl;

	@Value("${create.applicant.without.document.url}")
	private String createApplicantWithoutDocumentUrl;

	@Value("${nexus.buddy.client.config.url}")
	private String nexusBuddyClientConfigUrl;

	@Value("${nexus.buddy.tools.config.url}")
	private String nexusBuddyToolsConfigUrl;

	@Value("${nexus.buddy.tools.param.config.url}")
	private String nexusBuddyToolsParamConfigUrl;

	@Value("${nexus.buddy.dashboard.url}")
	private String nexusBuddyDashboardUrl;

	@Value("${nexus.buddy.client.insights.url}")
	private String nexusBuddyClientInsightsUrl;

	@Value("${nexus.buddy.chat.url}")
	private String nexusBuddyChatUrl;

	// Core Service URL
	@Value("${core.service.url:http://localhost:8082}")
	private String coreServiceUrl;

	// Core Service Endpoints
	@Value("${core.product.add.url}")
	private String coreProductAddUrl;

	@Value("${core.product.get.url}")
	private String coreProductGetUrl;

	@Value("${core.product.all.url}")
	private String coreProductAllUrl;

	@Value("${core.material.add.url}")
	private String coreMaterialAddUrl;

	@Value("${core.material.get.url}")
	private String coreMaterialGetUrl;

	@Value("${core.material.all.url}")
	private String coreMaterialAllUrl;

	@Value("${core.warehouse.add.url}")
	private String coreWarehouseAddUrl;

	@Value("${core.warehouse.get.url}")
	private String coreWarehouseGetUrl;

	@Value("${core.warehouse.all.url}")
	private String coreWarehouseAllUrl;

	@Value("${core.order.add.url}")
	private String coreOrderAddUrl;

	@Value("${core.order.get.url}")
	private String coreOrderGetUrl;

	@Value("${core.order.all.url}")
	private String coreOrderAllUrl;

	@Value("${core.partnership.add.url}")
	private String corePartnershipAddUrl;

	@Value("${core.partnership.get.url}")
	private String corePartnershipGetUrl;

	@Value("${core.partnership.all.url}")
	private String corePartnershipAllUrl;

	@Value("${core.partnership.status.url}")
	private String corePartnershipStatusUrl;

	@Value("${core.partnership.active.url}")
	private String corePartnershipActiveUrl;

	@Value("${core.partnership.update.status.url}")
	private String corePartnershipUpdateStatusUrl;

	// Partnership Invitation Endpoints
	@Value("${core.partnership.invitation.create.url}")
	private String corePartnershipInvitationCreateUrl;

	@Value("${core.partnership.invitation.respond.url}")
	private String corePartnershipInvitationRespondUrl;

	@Value("${core.partnership.invitation.get.url}")
	private String corePartnershipInvitationGetUrl;

	@Value("${core.partnership.invitation.sent.url}")
	private String corePartnershipInvitationSentUrl;

	@Value("${core.partnership.invitation.received.url}")
	private String corePartnershipInvitationReceivedUrl;

	@Value("${core.partnership.invitation.pending.url}")
	private String corePartnershipInvitationPendingUrl;

	@Value("${core.partnership.invitation.withdraw.url}")
	private String corePartnershipInvitationWithdrawUrl;

	// Supplier Discovery Endpoints
	@Value("${core.supplier.discover.url}")
	private String coreSupplierDiscoverUrl;

	@Value("${core.supplier.qualify.url}")
	private String coreSupplierQualifyUrl;

	@Value("${core.supplier.qualification.get.url}")
	private String coreSupplierQualificationGetUrl;

	@Value("${core.supplier.qualification.all.url}")
	private String coreSupplierQualificationAllUrl;

	// Supplier Management Endpoints
	@Value("${core.supplier.add.url}")
	private String coreSupplierAddUrl;

	@Value("${core.supplier.get.url}")
	private String coreSupplierGetUrl;

	@Value("${core.supplier.all.url}")
	private String coreSupplierAllUrl;

	@Value("${core.supplier.by.account.url}")
	private String coreSupplierByAccountUrl;

	// Keycloak URLs
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

	// Getters for OAuth2 properties
	public String getKeycloakOAuth2ClientId() {
		return keycloakOAuth2ClientId;
	}

	public String getKeycloakOAuth2ClientSecret() {
		return keycloakOAuth2ClientSecret;
	}

	public String getKeycloakOAuth2RedirectUri() {
		return keycloakOAuth2RedirectUri;
	}

	public boolean isKeycloakOAuth2Enabled() {
		return keycloakOAuth2Enabled;
	}

	public String getNexusBuddyClientInsightsUrl() {
		return nexusBuddyClientInsightsUrl;
	}

	public String getNexusBuddyChatUrl() {
		return nexusBuddyChatUrl;
	}

	public String getNexusBuddyClientConfigUrl() {
		return nexusBuddyClientConfigUrl;
	}

	public String getCoreServiceUrl() {
		return coreServiceUrl;
	}

	// Core Service Endpoints
	public String getCoreProductAddUrl() {
		return coreProductAddUrl;
	}

	public String getCoreProductGetUrl() {
		return coreProductGetUrl;
	}

	public String getCoreProductAllUrl() {
		return coreProductAllUrl;
	}

	public String getCoreMaterialAddUrl() {
		return coreMaterialAddUrl;
	}

	public String getCoreMaterialGetUrl() {
		return coreMaterialGetUrl;
	}

	public String getCoreMaterialAllUrl() {
		return coreMaterialAllUrl;
	}

	public String getCoreWarehouseAddUrl() {
		return coreWarehouseAddUrl;
	}

	public String getCoreWarehouseGetUrl() {
		return coreWarehouseGetUrl;
	}

	public String getCoreWarehouseAllUrl() {
		return coreWarehouseAllUrl;
	}

	public String getCoreOrderAddUrl() {
		return coreOrderAddUrl;
	}

	public String getCoreOrderGetUrl() {
		return coreOrderGetUrl;
	}

	public String getCoreOrderAllUrl() {
		return coreOrderAllUrl;
	}

	public String getCorePartnershipAddUrl() {
		return corePartnershipAddUrl;
	}

	public String getCorePartnershipGetUrl() {
		return corePartnershipGetUrl;
	}

	public String getCorePartnershipAllUrl() {
		return corePartnershipAllUrl;
	}

	public String getCorePartnershipStatusUrl() {
		return corePartnershipStatusUrl;
	}

	public String getCorePartnershipActiveUrl() {
		return corePartnershipActiveUrl;
	}

	public String getCorePartnershipUpdateStatusUrl() {
		return corePartnershipUpdateStatusUrl;
	}

	// Partnership Invitation Endpoints
	public String getCorePartnershipInvitationCreateUrl() {
		return corePartnershipInvitationCreateUrl;
	}

	public String getCorePartnershipInvitationRespondUrl() {
		return corePartnershipInvitationRespondUrl;
	}

	public String getCorePartnershipInvitationGetUrl() {
		return corePartnershipInvitationGetUrl;
	}

	public String getCorePartnershipInvitationSentUrl() {
		return corePartnershipInvitationSentUrl;
	}

	public String getCorePartnershipInvitationReceivedUrl() {
		return corePartnershipInvitationReceivedUrl;
	}

	public String getCorePartnershipInvitationPendingUrl() {
		return corePartnershipInvitationPendingUrl;
	}

	public String getCorePartnershipInvitationWithdrawUrl() {
		return corePartnershipInvitationWithdrawUrl;
	}

	// Supplier Discovery Endpoints
	public String getCoreSupplierDiscoverUrl() {
		return coreSupplierDiscoverUrl;
	}

	public String getCoreSupplierQualifyUrl() {
		return coreSupplierQualifyUrl;
	}

	public String getCoreSupplierQualificationGetUrl() {
		return coreSupplierQualificationGetUrl;
	}

	public String getCoreSupplierQualificationAllUrl() {
		return coreSupplierQualificationAllUrl;
	}

	// Supplier Management Endpoints
	public String getCoreSupplierAddUrl() {
		return coreSupplierAddUrl;
	}

	public String getCoreSupplierGetUrl() {
		return coreSupplierGetUrl;
	}

	public String getCoreSupplierAllUrl() {
		return coreSupplierAllUrl;
	}

	public String getCoreSupplierByAccountUrl() {
		return coreSupplierByAccountUrl;
	}
}
