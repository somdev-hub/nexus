package com.nexus.iam.utils;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class WebConstants {

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
}
