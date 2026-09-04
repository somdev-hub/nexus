package com.nexus.core.entities;

import java.sql.Timestamp;

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

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_shipment_stops", schema = "core")
public class ShipmentStop extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stop_id")
	private Long stopId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", referencedColumnName = "shipment_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Shipment shipment;

	@Column(name = "stop_sequence")
	private Integer stopSequence;

	@Enumerated(EnumType.STRING)
	@Column(name = "stop_type")
	private StopType stopType; // PICKUP, DELIVERY, INTERMEDIATE

	@Column(name = "location_name")
	private String locationName;

	@Column(name = "address")
	private String address;

	@Column(name = "contact_name")
	private String contactName;

	@Column(name = "contact_phone")
	private String contactPhone;

	@Column(name = "contact_email")
	private String contactEmail;

	@Column(name = "scheduled_arrival")
	private Timestamp scheduledArrival;

	@Column(name = "scheduled_departure")
	private Timestamp scheduledDeparture;

	@Column(name = "actual_arrival")
	private Timestamp actualArrival;

	@Column(name = "actual_departure")
	private Timestamp actualDeparture;

	@Enumerated(EnumType.STRING)
	@Column(name = "stop_status")
	private StopStatus stopStatus = StopStatus.PENDING;

	@Column(name = "notes")
	private String notes;

	@Version
	private Long version = 0L;
}