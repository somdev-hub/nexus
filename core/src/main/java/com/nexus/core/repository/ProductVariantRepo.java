package com.nexus.core.repository;

import com.nexus.core.entities.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantRepo extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findByVariantIdAndCatalogSupplierOrgAccountId(Long variantId, Long orgId);

    @Query("""
            SELECT v FROM ProductVariant v
            WHERE v.catalog.supplierOrg.accountId = :orgId
            AND (:catalogId IS NULL OR v.catalog.catalogId = :catalogId)
            AND (:variantType IS NULL OR v.variantType = :variantType)
            AND (:bomMaterialId IS NULL OR v.bomMaterial.materialId = :bomMaterialId)
            """)
    Page<ProductVariant> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("catalogId") Long catalogId,
            @Param("variantType") ProductVariant.VariantType variantType,
            @Param("bomMaterialId") Long bomMaterialId,
            Pageable pageable);
}
