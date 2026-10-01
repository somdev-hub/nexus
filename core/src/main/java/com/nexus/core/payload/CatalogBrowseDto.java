package com.nexus.core.payload;

import com.nexus.core.model.enums.CatalogAccessLevel;
import com.nexus.core.model.enums.CatalogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Dedicated read DTO for cross-org catalog discovery
 * ({@code GET /core/catalog/browse}).
 * <p>
 * Kept separate from {@link SupplierCatalogDto} so supplier-facing CRUD keeps its
 * contract while browse flattens supplier identity ({@code supplierOrgId} /
 * {@code supplierOrgName}) for retailer discovery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogBrowseDto {

    private Long catalogId;

    private String name;

    private String code;

    private String sku;

    private String description;

    private String category;

    private String family;

    private Double basePrice;

    private String currency;

    private CatalogStatus status;

    private CatalogAccessLevel accessLevel;

    private Boolean isPublished;

    private Timestamp publishedAt;

    private Long supplierOrgId;

    private String supplierOrgName;

    private Integer variantCount;

    private Integer priceTierCount;

    private Timestamp createdAt;

    private Timestamp updatedAt;
}
