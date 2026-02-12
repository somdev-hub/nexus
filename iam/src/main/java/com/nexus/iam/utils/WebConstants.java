package com.nexus.iam.utils;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class WebConstants {

    @Value("${dms.service.url}")
    public String dmsServiceUrl;

    @Value("${individual.dms.url}")
    public String individualFileUploadUrl;

    @Value("${org.dms.url}")
    public String orgFileUploadUrl;

    @Value("${common.dms.url}")
    public String commonDmsUrl;

    @Value("${generic.user.id}")
    public String genericUserId;

    @Value("${generic.password}")
    public String genericPassword;

    @Value("${hr.init.url}")
    public String hrInitUrl;

    @Value("${hr.employee.paycheck.url}")
    public String employeePaycheckUrl;

    @Value("${hr.employee.paycheck.get.url}")
    public String employeePaycheckGetUrl;
}
