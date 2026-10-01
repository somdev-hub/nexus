package com.nexus.core.repository;

import com.nexus.core.model.enums.CatalogAccessLevel;
import com.nexus.core.model.enums.CatalogStatus;
import com.nexus.core.model.entities.SupplierCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
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

    /**
     * Cross-org discovery candidates for {@code GET /core/catalog/browse}.
     * <p>
     * Published-state gate: {@code status = PUBLISHED AND isPublished = true}
     * (publish sets both flags; archive/draft transitions clear
     * {@code isPublished}).
     * <p>
     * Access gate: {@code PUBLIC} visible to all; {@code PARTNER_ONLY} only when the
     * caller org id is an exact token of the comma-separated
     * {@code allowedPartnerOrgIds} (comma-delimited LIKE match, spaces stripped —
     * so org {@code 2} never matches {@code 12}); {@code PRIVATE} never matches.
     */
    @Query(value = """
            SELECT sc FROM SupplierCatalog sc
            LEFT JOIN FETCH sc.supplierOrg
            WHERE sc.status = com.nexus.core.model.enums.CatalogStatus.PUBLISHED
            AND sc.isPublished = true
            AND (sc.accessLevel = com.nexus.core.model.enums.CatalogAccessLevel.PUBLIC
                OR (sc.accessLevel = com.nexus.core.model.enums.CatalogAccessLevel.PARTNER_ONLY
                    AND sc.allowedPartnerOrgIds IS NOT NULL
                    AND CONCAT(',', REPLACE(sc.allowedPartnerOrgIds, ' ', ''), ',') LIKE CONCAT('%,', :callerOrgStr, ',%')))
            AND (:category IS NULL OR sc.category = :category)
            AND (:family IS NULL OR sc.family = :family)
            AND (:supplierOrgId IS NULL OR sc.supplierOrg.accountId = :supplierOrgId)
            AND (:search IS NULL OR :search = '' OR LOWER(CAST(sc.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(CAST(sc.code AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(CAST(sc.sku AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(sc) FROM SupplierCatalog sc
            WHERE sc.status = com.nexus.core.model.enums.CatalogStatus.PUBLISHED
            AND sc.isPublished = true
            AND (sc.accessLevel = com.nexus.core.model.enums.CatalogAccessLevel.PUBLIC
                OR (sc.accessLevel = com.nexus.core.model.enums.CatalogAccessLevel.PARTNER_ONLY
                    AND sc.allowedPartnerOrgIds IS NOT NULL
                    AND CONCAT(',', REPLACE(sc.allowedPartnerOrgIds, ' ', ''), ',') LIKE CONCAT('%,', :callerOrgStr, ',%')))
            AND (:category IS NULL OR sc.category = :category)
            AND (:family IS NULL OR sc.family = :family)
            AND (:supplierOrgId IS NULL OR sc.supplierOrg.accountId = :supplierOrgId)
            AND (:search IS NULL OR :search = '' OR LOWER(CAST(sc.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(CAST(sc.code AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(CAST(sc.sku AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<SupplierCatalog> findBrowseCandidates(
            @Param("callerOrgStr") String callerOrgStr,
            @Param("category") String category,
            @Param("family") String family,
            @Param("supplierOrgId") Long supplierOrgId,
            @Param("search") String search,
            Pageable pageable);
}
