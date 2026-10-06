package com.nexus.hr.model.entities;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.nexus.hr.model.enums.OrgAddressType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_org_addresses", schema = "hr")
@Data
public class OrgAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orgAddressId;

    private Long orgId;

    // Short label shown in dropdowns, e.g. "Mumbai Warehouse".
    private String label;

    @Enumerated(EnumType.STRING)
    private OrgAddressType addressType;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String country;

    private String pincode;

    private String contactName;

    private String contactPhone;

    // At most one default billing / shipping address per org.
    private Boolean isDefaultBilling = false;

    private Boolean isDefaultShipping = false;

    private Boolean isActive;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        isActive = true;
        if (isDefaultBilling == null) {
            isDefaultBilling = false;
        }
        if (isDefaultShipping == null) {
            isDefaultShipping = false;
        }
    }
}
