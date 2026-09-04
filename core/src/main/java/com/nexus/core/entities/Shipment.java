package com.nexus.core.entities;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_shipments", schema = "core")
public class Shipment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "shipment_id")
	private Long shipmentId;

	@Column(name = "shipment_number", unique = true)
	private String shipmentNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "retailer_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account retailerOrg;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account supplierOrg;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account logisticsOrg;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partnership_id", referencedColumnName = "partnership_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Partnership partnership;

	@Enumerated(EnumType.STRING)
	private ShipmentStatus status = ShipmentStatus.DRAFT;

	@Enumerated(EnumType.STRING)
	@Column(name = "shipment_mode")
	private ShipmentMode shipmentMode;

	@Column(name = "incoterms")
	private String incoterms;

	// Pickup details
	@Column(name = "pickup_location")
	private String pickupLocation;

	@Column(name = "pickup_address")
	private String pickupAddress;

	@Column(name = "pickup_contact_name")
	private String pickupContactName;

	@Column(name = "pickup_contact_phone")
	private String pickupContactPhone;

	@Column(name = "pickup_contact_email")
	private String pickupContactEmail;

	@Column(name = "pickup_date")
	private Date pickupDate;

	@Column(name = "pickup_time_window_start")
	private Timestamp pickupTimeWindowStart;

	@Column(name = "pickup_time_window_end")
	private Timestamp pickupTimeWindowEnd;

	// Delivery details
	@Column(name = "delivery_location")
	private String deliveryLocation;

	@Column(name = "delivery_address")
	private String deliveryAddress;

	@Column(name = "delivery_contact_name")
	private String deliveryContactName;

	@Column(name = "delivery_contact_phone")
	private String deliveryContactPhone;

	@Column(name = "delivery_contact_email")
	private String deliveryContactEmail;

	@Column(name = "delivery_date")
	private Date deliveryDate;

	@Column(name = "delivery_time_window_start")
	private Timestamp deliveryTimeWindowStart;

	@Column(name = "delivery_time_window_end")
	private Timestamp deliveryTimeWindowEnd;

	// Shipment details
	@Column(name = "total_weight")
	private Double totalWeight;

	@Column(name = "total_volume")
	private Double totalVolume;

	@Column(name = "total_packages")
	private Integer totalPackages;

	@Column(name = "package_type")
	private String packageType;

	@Column(name = "special_instructions")
	private String specialInstructions;

	@Column(name = "hazardous_material")
	private Boolean hazardousMaterial = false;

	@Column(name = "temperature_controlled")
	private Boolean temperatureControlled = false;

	@Column(name = "min_temperature")
	private Double minTemperature;

	@Column(name = "max_temperature")
	private Double maxTemperature;

	// Freight cost
	@Column(name = "freight_cost")
	private Double freightCost;

	@Column(name = "actual_freight_cost")
	private Double actualFreightCost;

	@Column(name = "currency")
	private String currency = "USD";

	@Column(name = "freight_terms")
	private String freightTerms; // PREPAID, COLLECT, THIRD_PARTY

	// Tracking
	@Column(name = "tracking_number")
	private String trackingNumber;

	@Column(name = "carrier_name")
	private String carrierName;

	@Column(name = "carrier_reference")
	private String carrierReference;

	@Column(name = "estimated_departure")
	private Timestamp estimatedDeparture;

	@Column(name = "actual_departure")
	private Timestamp actualDeparture;

	@Column(name = "estimated_arrival")
	private Timestamp estimatedArrival;

	@Column(name = "actual_arrival")
	private Timestamp actualArrival;

	@Column(name = "notes")
	private String notes;

	@Version
	private Long version = 0L;

	@OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<ShipmentStop> stops = new ArrayList<>();

	@OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<TrackingEvent> trackingEvents = new ArrayList<>();

	@OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<ShipmentDocument> documents = new ArrayList<>();

	// Helper methods
	public void addStop(ShipmentStop stop) {
		stops.add(stop);
		stop.setShipment(this);
	}

	public void removeStop(ShipmentStop stop) {
		stops.remove(stop);
		stop.setShipment(null);
	}

	public void addTrackingEvent(TrackingEvent event) {
		trackingEvents.add(event);
		event.setShipment(this);
	}

	public void addDocument(ShipmentDocument document) {
		documents.add(document);
		document.setShipment(this);
	}
}