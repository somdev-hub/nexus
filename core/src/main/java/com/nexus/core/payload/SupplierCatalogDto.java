package com.nexus.core.payload;

import com.nexus.core.entities.CatalogAccessLevel;
import com.nexus.core.entities.CatalogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierCatalogDto {

    private Long catalogId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Code is required")
    private String code;

    private String description;

    private String category;

    private String family;

    private String sku;

    private String attributes;

    private String specifications;

    private Double basePrice;

    private String currency;

    private CatalogStatus status;

    private CatalogAccessLevel accessLevel;

    private Boolean isPublished;

    private Timestamp publishedAt;

    private String allowedPartnerOrgIds;

    private Long supplierOrgId;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
