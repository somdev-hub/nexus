package com.nexus.hr.utils;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class WOWOProperties {

    @Value("${wowo.should.attendance.deduction.work}")
    private boolean shouldAttendanceDeductionWork;
}
