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
@Table(name = "t_tracking_events", schema = "core")
public class TrackingEvent extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "event_id")
	private Long eventId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", referencedColumnName = "shipment_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Shipment shipment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stop_id", referencedColumnName = "stop_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private ShipmentStop stop;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type")
	private TrackingEventType eventType;

	@Column(name = "event_timestamp")
	private Timestamp eventTimestamp;

	@Column(name = "location")
	private String location;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

	@Column(name = "description")
	private String description;

	@Column(name = "reported_by")
	private String reportedBy;

	@Column(name = "source")
	private String source; // MANUAL, GPS, CARRIER_API, EDI

	@Version
	private Long version = 0L;
}