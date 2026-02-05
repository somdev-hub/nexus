package com.nexus.hr.model.entities;

import com.nexus.hr.model.enums.EmployeeLevel;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "t_employee_paychecks", schema = "hr")
public class EmployeePaycheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeePaycheckId;

    private Long orgId;

    private String role;

    private Long deptId;

    @Enumerated(EnumType.STRING)
    private EmployeeLevel employeeLevel;

    private Double minBasePay;

    private Double maxBasePay;

    private Double minTotalBonuses;

    private Double maxTotalBonuses;

    private Double minTotalDeductions;

    private Double maxTotalDeductions;

    private String minAnnualSalary;

    private String maxAnnualSalary;
}
