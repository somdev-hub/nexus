package com.nexus.core.payload;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.nexus.core.model.enums.ProductCategory;
import com.nexus.core.model.enums.ProductStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductDto {
	// Returned on reads; null on create. Required so the UI can link
	// list rows to detail/edit pages (without it every row maps to id 0).
	private Long productId;

	@NotBlank(message = "Product name is required")
	private String name;

	@NotBlank(message = "Product code is required")
	private String code;

	private String description;

	private List<MultipartFile> productImages;

	private List<MaterialRequirementDto> materialRequirements;

	// Set server-side from OrganizationContextHolder in ProductController; must
	// not be required from the client or @Valid rejects every create.
	private Long org;

	private Long productManager;

	private Double sellingPrice;

	private Double cost;

	private Boolean taxCharged;

	private Double taxPercentage;

	private ProductStatus productStatus;

	private ProductCategory productCategory;

	private Double price;

	// Retailer product form fields (nexus-suite retailer/products/add)
	private String subCategory;

	private String brand;

	private String unitOfMeasure;

	private String currency;

	private Integer minOrderQuantity;

	private Integer maxOrderQuantity;

	private Integer leadTimeDays;

	private Double weight;

	private String dimensions;

	private String barcode;

	private String sku;

	private List<String> tags;
}
