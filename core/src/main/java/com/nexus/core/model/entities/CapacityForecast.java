package com.nexus.core.model.entities;

import com.nexus.core.model.enums.CapacityUnit;
import com.nexus.core.model.enums.FleetAssetType;
import jakarta.persistence.Column;
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

import java.sql.Date;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_capacity_forecasts", schema = "core")
public class CapacityForecast extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forecast_id")
    private Long forecastId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account logisticsOrg;

    @Column(name = "origin_lane")
    private String originLane;

    @Column(name = "destination_lane")
    private String destinationLane;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type")
    private FleetAssetType equipmentType;

    @Column(name = "period_start")
    private Date periodStart;

    @Column(name = "period_end")
    private Date periodEnd;

    @Column(name = "available_capacity")
    private Double availableCapacity = 0.0;

    @Column(name = "booked_capacity")
    private Double bookedCapacity = 0.0;

    // Price per capacityUnit for this lane/period.
    @Column(name = "unit_price")
    private Double unitPrice;

    @Column(name = "currency")
    private String currency = "USD";

    // Unit for the capacity quantities above (KG, LBS, LITRES,
    // SHIPPING_CONTAINER, FREIGHT_CONTAINER, ...). Defaults to KG so
    // legacy rows keep their meaning.
    @Enumerated(EnumType.STRING)
    @Column(name = "capacity_unit")
    private CapacityUnit capacityUnit = CapacityUnit.KG;

    // Per-unit specifications. Required when capacityUnit is unitized
    // (PALLETS, SHIPPING_CONTAINER, FREIGHT_CONTAINER): internal
    // length/width/height plus total volume of one unit.
    @Column(name = "unit_length")
    private Double unitLength;

    @Column(name = "unit_width")
    private Double unitWidth;

    @Column(name = "unit_height")
    private Double unitHeight;

    // Dimension UoM for length/width/height: M (meters) or FT (feet).
    @Column(name = "dimension_uom")
    private String dimensionUom = "M";

    @Column(name = "unit_volume")
    private Double unitVolume;

    // Volume UoM for unitVolume: CBM, CFT or L.
    @Column(name = "volume_uom")
    private String volumeUom = "CBM";

    // Private route capacity: when set, this lane belongs to one
    // long-term partnership only and is hidden from the public
    // marketplace (other suppliers never see it).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id", referencedColumnName = "partnership_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Partnership partnership;

    private String notes;

    @Version
    private Long version = 0L;
}
