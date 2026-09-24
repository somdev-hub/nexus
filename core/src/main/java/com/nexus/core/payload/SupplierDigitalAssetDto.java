package com.nexus.core.payload;

import com.nexus.core.entities.SupplierDigitalAsset;
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
public class SupplierDigitalAssetDto {

    private Long assetId;

    @NotNull(message = "Catalog ID is required")
    private Long catalogId;

    private String catalogName;

    @NotNull(message = "Asset type is required")
    private SupplierDigitalAsset.AssetType assetType;

    private String fileName;

    private String dmsDocumentId;

    private String dmsDocumentUrl;

    private Integer version;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
