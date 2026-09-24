package com.nexus.core.repository;

import com.nexus.core.entities.CatalogAccessLevel;
import com.nexus.core.entities.CatalogStatus;
import com.nexus.core.entities.SupplierCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierCatalogRepo extends JpaRepository<SupplierCatalog, Long> {

    Optional<SupplierCatalog> findByCatalogIdAndSupplierOrgAccountId(Long catalogId, Long orgId);

    Optional<SupplierCatalog> findByCodeAndSupplierOrgAccountId(String code, Long orgId);

    Optional<SupplierCatalog> findBySkuAndSupplierOrgAccountId(String sku, Long orgId);

    boolean existsByCodeAndSupplierOrgAccountId(String code, Long orgId);

    boolean existsBySkuAndSupplierOrgAccountId(String sku, Long orgId);

    @Query("""
            SELECT sc FROM SupplierCatalog sc
            WHERE sc.supplierOrg.accountId = :orgId
            AND (:status IS NULL OR sc.status = :status)
            AND (:category IS NULL OR sc.category = :category)
            AND (:family IS NULL OR sc.family = :family)
            AND (:accessLevel IS NULL OR sc.accessLevel = :accessLevel)
            AND (:isPublished IS NULL OR sc.isPublished = :isPublished)
            AND (:search IS NULL OR :search = '' OR LOWER(CAST(sc.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(CAST(sc.code AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(CAST(sc.sku AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<SupplierCatalog> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") CatalogStatus status,
            @Param("category") String category,
            @Param("family") String family,
            @Param("accessLevel") CatalogAccessLevel accessLevel,
            @Param("isPublished") Boolean isPublished,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(sc) FROM SupplierCatalog sc WHERE sc.supplierOrg.accountId = :orgId AND sc.status = :status")
    long countByOrgAndStatus(@Param("orgId") Long orgId, @Param("status") CatalogStatus status);
}
