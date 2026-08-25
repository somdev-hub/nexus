package com.nexus.core.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SupplierQualificationDto {

	@NotNull(message = "Supplier ID is required")
	private Long supplierId;

	@NotNull(message = "Retailer organization ID is required")
	private Long retailerOrgId;

	@NotBlank(message = "Compliance documents are required")
	@Size(max = 2000, message = "Compliance documents must not exceed 2000 characters")
	private String complianceDocuments; // JSON array of DMS document IDs

	@Size(max = 1000, message = "Compliance notes must not exceed 1000 characters")
	private String complianceNotes;

	@Size(max = 100, message = "Assessed by must not exceed 100 characters")
	private String assessedBy;
}