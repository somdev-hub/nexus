package com.nexus.core.entities;

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
@Table(name = "t_shipment_documents", schema = "core")
public class ShipmentDocument extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "document_id")
	private Long documentId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", referencedColumnName = "shipment_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Shipment shipment;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type")
	private ShipmentDocumentType documentType;

	@Column(name = "document_name")
	private String documentName;

	@Column(name = "dms_document_id")
	private String dmsDocumentId;

	@Column(name = "dms_version")
	private String dmsVersion;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "file_size")
	private Long fileSize;

	@Column(name = "mime_type")
	private String mimeType;

	@Column(name = "uploaded_by")
	private String uploadedBy;

	@Version
	private Long version = 0L;
}