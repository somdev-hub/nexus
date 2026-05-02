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

    @Value("${today.applied.hr.requests.url}")
    private String todayAppliedHrRequestsUrl;

    @Value("${hr.recuitment.url}")
    private String hrRecruitmentUrl;

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

}
