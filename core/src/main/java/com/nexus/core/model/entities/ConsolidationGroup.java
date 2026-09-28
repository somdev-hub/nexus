package com.nexus.core.model.entities;

import com.nexus.core.model.enums.ConsolidationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_consolidation_groups", schema = "core")
public class ConsolidationGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_number", unique = true, nullable = false)
    private String groupNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account logisticsOrg;

    @Enumerated(EnumType.STRING)
    private ConsolidationStatus status = ConsolidationStatus.OPEN;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "t_consolidation_shipments", schema = "core",
            joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "group_id"))
    @Column(name = "shipment_id")
    private List<Long> shipmentIds = new ArrayList<>();

    @Column(name = "total_weight")
    private Double totalWeight = 0.0;

    @Column(name = "total_volume")
    private Double totalVolume = 0.0;

    @Column(name = "utilization_pct")
    private Double utilizationPct = 0.0;

    private String notes;

    @Version
    private Long version = 0L;
}
