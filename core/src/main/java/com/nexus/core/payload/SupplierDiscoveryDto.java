package com.nexus.core.payload;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SupplierDiscoveryDto {

	@Size(max = 100, message = "Category must not exceed 100 characters")
	private String category;

	@Size(max = 255, message = "Location must not exceed 255 characters")
	private String location;

	@Min(value = 0, message = "Minimum rating must be between 0 and 5")
	@Max(value = 5, message = "Minimum rating must be between 0 and 5")
	private Double minRating;

	@Size(max = 500, message = "Certifications must not exceed 500 characters")
	private String certifications; // Comma-separated

	private List<String> certificationList; // Alternative: list of certifications
}