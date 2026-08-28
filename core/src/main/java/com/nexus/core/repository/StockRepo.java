package com.nexus.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Material;
import com.nexus.core.entities.Product;
import com.nexus.core.entities.Stock;
import com.nexus.core.entities.Stock.ValuationMethod;
import com.nexus.core.entities.Warehouse;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Stock entity.
 * Supports FR-RET-010: Multi-Warehouse Inventory
 * Supports FR-RET-011: Reorder Point Automation
 */
@Repository
public interface StockRepo extends JpaRepository<Stock, Long> {

	Optional<Stock> findByMaterialAndWarehouse(Material material, Warehouse warehouse);

	@Query("SELECT s FROM Stock s WHERE s.material = :material AND s.warehouse = :warehouse AND s.isActive = true")
	Optional<Stock> findActiveByMaterialAndWarehouse(@Param("material") Material material,
			@Param("warehouse") Warehouse warehouse);

	@Query("SELECT s FROM Stock s WHERE s.warehouse = :warehouse AND s.isActive = true")
	List<Stock> findActiveByWarehouse(@Param("warehouse") Warehouse warehouse);

	@Query("SELECT s FROM Stock s WHERE s.material = :material AND s.isActive = true")
	List<Stock> findActiveByMaterial(@Param("material") Material material);

	@Query("SELECT s FROM Stock s WHERE s.warehouse.org = :orgId AND s.isActive = true")
	List<Stock> findActiveByOrgId(@Param("orgId") Long orgId);

	@Query("SELECT s FROM Stock s WHERE s.warehouse.org = :orgId AND s.isActive = true")
	Page<Stock> findActiveByOrgId(@Param("orgId") Long orgId, Pageable pageable);

	@Query("SELECT s FROM Stock s WHERE s.warehouse.org = :orgId AND s.isActive = true AND s.quantityAvailable <= s.reorderPoint AND s.reorderPoint IS NOT NULL")
	List<Stock> findBelowReorderPoint(@Param("orgId") Long orgId);

	@Query("SELECT s FROM Stock s WHERE s.warehouse.org = :orgId AND s.isActive = true AND s.quantityAvailable <= s.minStockLevel AND s.minStockLevel IS NOT NULL")
	List<Stock> findAtOrBelowMinLevel(@Param("orgId") Long orgId);

	@Query("SELECT s FROM Stock s WHERE s.warehouse.org = :orgId AND s.isActive = true AND s.valuationMethod = :method")
	List<Stock> findByValuationMethod(@Param("orgId") Long orgId, @Param("method") ValuationMethod method);

	@Query("SELECT s FROM Stock s WHERE s.material.product = :product AND s.warehouse.org = :orgId AND s.isActive = true")
	List<Stock> findByProductAndOrg(@Param("product") Product product, @Param("orgId") Long orgId);

	@Query("SELECT SUM(s.quantityOnHand * s.averageCost) FROM Stock s WHERE s.warehouse.org = :orgId AND s.isActive = true")
	Double getTotalInventoryValue(@Param("orgId") Long orgId);

	@Query("SELECT SUM(s.quantityOnHand * s.averageCost) FROM Stock s WHERE s.warehouse = :warehouse AND s.isActive = true")
	Double getWarehouseInventoryValue(@Param("warehouse") Warehouse warehouse);
}