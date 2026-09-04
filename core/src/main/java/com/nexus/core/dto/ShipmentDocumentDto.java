package com.nexus.core.dto;

import com.nexus.core.entities.ShipmentDocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDocumentDto {
	private Long documentId;

	@NotNull(message = "Shipment ID is required")
	private Long shipmentId;

	@NotNull(message = "Document type is required")
	private ShipmentDocumentType documentType;

	@NotNull(message = "DMS document ID is required")
	private String dmsDocumentId;

	@Size(max = 255, message = "Document name must not exceed 255 characters")
	private String documentName;

	@Size(max = 100, message = "Document version must not exceed 100 characters")
	private String documentVersion;

	private String mimeType;
	private Long fileSize;
	private String checksum;

	private LocalDateTime documentDate;
	private LocalDateTime expiryDate;

	@Size(max = 500, message = "Description must not exceed 500 characters")
	private String description;

	private String issuedBy;
	private String issuedTo;
	private String referenceNumber;

	private Boolean isRequired;
	private Boolean isVerified;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Long createdBy;
	private Long updatedBy;
}