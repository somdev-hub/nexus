package com.nexus.core.payload;

import com.nexus.core.entities.ProductVariant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDto {

    private Long variantId;

    @NotNull(message = "Catalog ID is required")
    private Long catalogId;

    private String catalogName;

    @NotNull(message = "Variant type is required")
    private ProductVariant.VariantType variantType;

    @NotBlank(message = "Variant value is required")
    private String variantValue;

    private String skuSuffix;

    private Double priceAdjustment;

    private Long bomMaterialId;

    private String bomMaterialName;

    private Double quantityAvailable;

    private Boolean isActive;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
