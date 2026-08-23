package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "t_payroll_deductions", schema = "hr")
public class PayrollDeductions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payrollDeductionsId;

    private String deductionType;

    private Double amount;

    private String description;

    @CreationTimestamp
    private Timestamp createdAt;
    @UpdateTimestamp
    private Timestamp updatedAt;
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_id", nullable = false)
    @JsonBackReference("payroll-deductions")
    private Payroll payroll;

    @PrePersist
    public void prePersist() {
        isActive = true;
    }
}