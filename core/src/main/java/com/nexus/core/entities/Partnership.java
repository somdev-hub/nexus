package com.nexus.core.entities;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "t_partnerships", schema = "core")
public class Partnership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_org", referencedColumnName = "id")
    private Account primaryOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secondary_org", referencedColumnName = "id")
    private Account secondaryOrg;

    private String partnershipTerm;

    private Double discountRate;

    @Enumerated(EnumType.STRING)
    private PartnershipStatus status;

    private Timestamp startDate;

    private Timestamp endDate;

    private Timestamp revivedDate;

}
