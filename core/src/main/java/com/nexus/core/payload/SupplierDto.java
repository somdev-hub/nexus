package com.nexus.core.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierDto {

	@NotNull(message = "Account ID is required")
	private Long accountId;

	@NotBlank(message = "Business name is required")
	@Size(max = 255, message = "Business name must not exceed 255 characters")
	private String businessName;

	@Size(max = 100, message = "Category must not exceed 100 characters")
	private String category;

	@Size(max = 255, message = "Location must not exceed 255 characters")
	private String location;

	@Size(max = 255, message = "Website must not exceed 255 characters")
	private String website;

	@Size(max = 100, message = "Contact person must not exceed 100 characters")
	private String contactPerson;

	@Size(max = 255, message = "Contact email must not exceed 255 characters")
	private String contactEmail;

	@Size(max = 50, message = "Contact phone must not exceed 50 characters")
	private String contactPhone;

	@Size(max = 500, message = "Certifications must not exceed 500 characters")
	private String certifications;
}