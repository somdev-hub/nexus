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

	// Partnership Agreement Endpoints
	@Value("${core.partnership.agreement.upload.url}")
	private String corePartnershipAgreementUploadUrl;

	@Value("${core.partnership.agreement.get.url}")
	private String corePartnershipAgreementGetUrl;

	@Value("${core.partnership.agreement.delete.url}")
	private String corePartnershipAgreementDeleteUrl;

	// Partnership Lifecycle Transition Endpoint
	@Value("${core.partnership.transition.url}")
	private String corePartnershipTransitionUrl;

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

	@Value("${core.supplier.qualification.update-status.url}")
	private String coreSupplierQualificationUpdateStatusUrl;

	// Supplier Management Endpoints
	@Value("${core.supplier.add.url}")
	private String coreSupplierAddUrl;

	@Value("${core.supplier.get.url}")
	private String coreSupplierGetUrl;

	@Value("${core.supplier.all.url}")
	private String coreSupplierAllUrl;

	// Purchase Order Endpoints
	@Value("${core.purchase.order.create.url}")
	private String corePurchaseOrderCreateUrl;

	@Value("${core.purchase.order.get.url}")
	private String corePurchaseOrderGetUrl;

	@Value("${core.purchase.order.all.url}")
	private String corePurchaseOrderAllUrl;

	@Value("${core.purchase.order.update.url}")
	private String corePurchaseOrderUpdateUrl;

	@Value("${core.purchase.order.transition.url}")
	private String corePurchaseOrderTransitionUrl;

	@Value("${core.purchase.order.amend.url}")
	private String corePurchaseOrderAmendUrl;

	@Value("${core.purchase.order.amendments.url}")
	private String corePurchaseOrderAmendmentsUrl;

	// Stock/Inventory Endpoints
	@Value("${core.stock.add.url}")
	private String coreStockAddUrl;

	@Value("${core.stock.get.url}")
	private String coreStockGetUrl;

	@Value("${core.stock.all.url}")
	private String coreStockAllUrl;

	@Value("${core.stock.valuation.url}")
	private String coreStockValuationUrl;

	@Value("${core.stock.warehouse-valuation.url}")
	private String coreStockWarehouseValuationUrl;

	@Value("${core.stock.adjust.url}")
	private String coreStockAdjustUrl;

	@Value("${core.stock.reserve.url}")
	private String coreStockReserveUrl;

	@Value("${core.stock.release-reservation.url}")
	private String coreStockReleaseReservationUrl;

	@Value("${core.stock.transfer.url}")
	private String coreStockTransferUrl;

	@Value("${core.stock.cycle-count.url}")
	private String coreStockCycleCountUrl;

	@Value("${core.stock.reorder-suggestions.url}")
	private String coreStockReorderSuggestionsUrl;

	@Value("${core.stock.settings.url}")
	private String coreStockSettingsUrl;

	// Stock Movement Endpoints
	@Value("${core.stock-movement.add.url}")
	private String coreStockMovementAddUrl;

	@Value("${core.stock-movement.get.url}")
	private String coreStockMovementGetUrl;

	@Value("${core.stock-movement.stock.url}")
	private String coreStockMovementStockUrl;

	@Value("${core.stock-movement.all.url}")
	private String coreStockMovementAllUrl;

	@Value("${core.stock-movement.summary.url}")
	private String coreStockMovementSummaryUrl;

	// Goods Receipt Endpoints
	@Value("${core.goods-receipt.create.url}")
	private String coreGoodsReceiptCreateUrl;

	@Value("${core.goods-receipt.get.url}")
	private String coreGoodsReceiptGetUrl;

	@Value("${core.goods-receipt.all.url}")
	private String coreGoodsReceiptAllUrl;

	@Value("${core.goods-receipt.update.url}")
	private String coreGoodsReceiptUpdateUrl;

	@Value("${core.goods-receipt.transition.url}")
	private String coreGoodsReceiptTransitionUrl;

	// Invoice Endpoints
	@Value("${core.invoice.create.url}")
	private String coreInvoiceCreateUrl;

	@Value("${core.invoice.get.url}")
	private String coreInvoiceGetUrl;

	@Value("${core.invoice.all.url}")
	private String coreInvoiceAllUrl;

	@Value("${core.invoice.update.url}")
	private String coreInvoiceUpdateUrl;

	@Value("${core.invoice.transition.url}")
	private String coreInvoiceTransitionUrl;

	// Three-Way Matching Endpoints
	@Value("${core.three-way-match.match.url}")
	private String coreThreeWayMatchMatchUrl;

	@Value("${core.three-way-match.can-invoice.url}")
	private String coreThreeWayMatchCanInvoiceUrl;

	@Value("${core.three-way-match.summary.url}")
	private String coreThreeWayMatchSummaryUrl;

	@Value("${core.three-way-match.validate-invoice.url}")
	private String coreThreeWayMatchValidateInvoiceUrl;

	// Supplier Performance Endpoints
	@Value("${core.supplier-performance.create.url}")
	private String coreSupplierPerformanceCreateUrl;

	@Value("${core.supplier-performance.get.url}")
	private String coreSupplierPerformanceGetUrl;

	@Value("${core.supplier-performance.by-supplier.url}")
	private String coreSupplierPerformanceBySupplierUrl;

	@Value("${core.supplier-performance.all.url}")
	private String coreSupplierPerformanceAllUrl;

	@Value("${core.supplier-performance.by-period.url}")
	private String coreSupplierPerformanceByPeriodUrl;

	@Value("${core.supplier-performance.by-supplier-period.url}")
	private String coreSupplierPerformanceBySupplierAndPeriodUrl;

	@Value("${core.supplier-performance.by-tier.url}")
	private String coreSupplierPerformanceByTierUrl;

	@Value("${core.supplier-performance.latest.url}")
	private String coreSupplierPerformanceLatestUrl;

	@Value("${core.supplier-performance.summary.account.url}")
	private String coreSupplierPerformanceSummaryAccountUrl;

	@Value("${core.supplier-performance.summary.supplier.url}")
	private String coreSupplierPerformanceSummarySupplierUrl;

	@Value("${core.supplier-performance.calculate.url}")
	private String coreSupplierPerformanceCalculateUrl;

	@Value("${core.supplier-performance.update.url}")
	private String coreSupplierPerformanceUpdateUrl;

	@Value("${core.supplier-performance.delete.url}")
	private String coreSupplierPerformanceDeleteUrl;

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

	// Supplier Performance Getters
	public String getCoreSupplierPerformanceCreateUrl() {
		return coreServiceUrl + coreSupplierPerformanceCreateUrl;
	}

	public String getCoreSupplierPerformanceGetUrl() {
		return coreServiceUrl + coreSupplierPerformanceGetUrl;
	}

	public String getCoreSupplierPerformanceBySupplierUrl() {
		return coreServiceUrl + coreSupplierPerformanceBySupplierUrl;
	}

	public String getCoreSupplierPerformanceAllUrl() {
		return coreServiceUrl + coreSupplierPerformanceAllUrl;
	}

	public String getCoreSupplierPerformanceByPeriodUrl() {
		return coreServiceUrl + coreSupplierPerformanceByPeriodUrl;
	}

	public String getCoreSupplierPerformanceBySupplierAndPeriodUrl() {
		return coreServiceUrl + coreSupplierPerformanceBySupplierAndPeriodUrl;
	}

	public String getCoreSupplierPerformanceByTierUrl() {
		return coreServiceUrl + coreSupplierPerformanceByTierUrl;
	}

	public String getCoreSupplierPerformanceLatestUrl() {
		return coreServiceUrl + coreSupplierPerformanceLatestUrl;
	}

	public String getCoreSupplierPerformanceSummaryAccountUrl() {
		return coreServiceUrl + coreSupplierPerformanceSummaryAccountUrl;
	}

	public String getCoreSupplierPerformanceSummarySupplierUrl() {
		return coreServiceUrl + coreSupplierPerformanceSummarySupplierUrl;
	}

	public String getCoreSupplierPerformanceCalculateUrl() {
		return coreServiceUrl + coreSupplierPerformanceCalculateUrl;
	}

	public String getCoreSupplierPerformanceUpdateUrl() {
		return coreServiceUrl + coreSupplierPerformanceUpdateUrl;
	}

	public String getCoreSupplierPerformanceDeleteUrl() {
		return coreServiceUrl + coreSupplierPerformanceDeleteUrl;
	}

	// Supplier Contract Endpoints
	@Value("${core.supplier-contract.create.url}")
	private String coreSupplierContractCreateUrl;

	@Value("${core.supplier-contract.get.url}")
	private String coreSupplierContractGetUrl;

	@Value("${core.supplier-contract.get-by-number.url}")
	private String coreSupplierContractGetByNumberUrl;

	@Value("${core.supplier-contract.all.url}")
	private String coreSupplierContractAllUrl;

	@Value("${core.supplier-contract.expiring.url}")
	private String coreSupplierContractExpiringUrl;

	@Value("${core.supplier-contract.auto-renewal.url}")
	private String coreSupplierContractAutoRenewalUrl;

	@Value("${core.supplier-contract.active-by-supplier.url}")
	private String coreSupplierContractActiveBySupplierUrl;

	@Value("${core.supplier-contract.summary.url}")
	private String coreSupplierContractSummaryUrl;

	@Value("${core.supplier-contract.update.url}")
	private String coreSupplierContractUpdateUrl;

	@Value("${core.supplier-contract.status.url}")
	private String coreSupplierContractStatusUrl;

	@Value("${core.supplier-contract.document.url}")
	private String coreSupplierContractDocumentUrl;

	@Value("${core.supplier-contract.approve.url}")
	private String coreSupplierContractApproveUrl;

	@Value("${core.supplier-contract.reject.url}")
	private String coreSupplierContractRejectUrl;

	@Value("${core.supplier-contract.terminate.url}")
	private String coreSupplierContractTerminateUrl;

	@Value("${core.supplier-contract.suspend.url}")
	private String coreSupplierContractSuspendUrl;

	@Value("${core.supplier-contract.renew.url}")
	private String coreSupplierContractRenewUrl;

	@Value("${core.supplier-contract.delete.url}")
	private String coreSupplierContractDeleteUrl;

	// Supplier Contract Getters
	public String getCoreSupplierContractAddUrl() {
		return coreServiceUrl + coreSupplierContractCreateUrl;
	}

	public String getCoreSupplierContractGetUrl() {
		return coreServiceUrl + coreSupplierContractGetUrl;
	}

	public String getCoreSupplierContractGetByNumberUrl() {
		return coreServiceUrl + coreSupplierContractGetByNumberUrl;
	}

	public String getCoreSupplierContractAllUrl() {
		return coreServiceUrl + coreSupplierContractAllUrl;
	}

	public String getCoreSupplierContractExpiringUrl() {
		return coreServiceUrl + coreSupplierContractExpiringUrl;
	}

	public String getCoreSupplierContractAutoRenewalUrl() {
		return coreServiceUrl + coreSupplierContractAutoRenewalUrl;
	}

	public String getCoreSupplierContractActiveBySupplierUrl() {
		return coreServiceUrl + coreSupplierContractActiveBySupplierUrl;
	}

	public String getCoreSupplierContractSummaryUrl() {
		return coreServiceUrl + coreSupplierContractSummaryUrl;
	}

	public String getCoreSupplierContractUpdateUrl() {
		return coreServiceUrl + coreSupplierContractUpdateUrl;
	}

	public String getCoreSupplierContractStatusUrl() {
		return coreServiceUrl + coreSupplierContractStatusUrl;
	}

	public String getCoreSupplierContractDocumentUrl() {
		return coreServiceUrl + coreSupplierContractDocumentUrl;
	}

	public String getCoreSupplierContractApproveUrl() {
		return coreServiceUrl + coreSupplierContractApproveUrl;
	}

	public String getCoreSupplierContractRejectUrl() {
		return coreServiceUrl + coreSupplierContractRejectUrl;
	}

	public String getCoreSupplierContractTerminateUrl() {
		return coreServiceUrl + coreSupplierContractTerminateUrl;
	}

	public String getCoreSupplierContractSuspendUrl() {
		return coreServiceUrl + coreSupplierContractSuspendUrl;
	}

	public String getCoreSupplierContractRenewUrl() {
		return coreServiceUrl + coreSupplierContractRenewUrl;
	}

	public String getCoreSupplierContractDeleteUrl() {
		return coreServiceUrl + coreSupplierContractDeleteUrl;
	}

}
