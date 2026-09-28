package com.nexus.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.nexus.core.model.entities.Material;
import com.nexus.core.model.entities.Stock;
import com.nexus.core.model.entities.StockMovement;
import com.nexus.core.model.entities.StockMovement.MovementType;
import com.nexus.core.model.entities.Warehouse;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Repository for StockMovement entity.
 * Provides audit trail queries for inventory movements.
 */
public interface StockMovementRepo extends JpaRepository<StockMovement, Long> {

	@Query("SELECT m FROM StockMovement m WHERE m.stock = :stock ORDER BY m.createdAt DESC")
	List<StockMovement> findByStockOrderByCreatedAtDesc(@Param("stock") Stock stock);

	@Query("SELECT m FROM StockMovement m WHERE m.stock = :stock ORDER BY m.createdAt DESC")
	Page<StockMovement> findByStockOrderByCreatedAtDesc(@Param("stock") Stock stock, Pageable pageable);

	@Query("SELECT m FROM StockMovement m WHERE m.stock.warehouse.org = :orgId ORDER BY m.createdAt DESC")
	List<StockMovement> findByOrgIdOrderByCreatedAtDesc(@Param("orgId") Long orgId);

	@Query("SELECT m FROM StockMovement m WHERE m.stock.warehouse.org = :orgId ORDER BY m.createdAt DESC")
	Page<StockMovement> findByOrgIdOrderByCreatedAtDesc(@Param("orgId") Long orgId, Pageable pageable);

	@Query("SELECT m FROM StockMovement m WHERE m.stock.warehouse = :warehouse ORDER BY m.createdAt DESC")
	List<StockMovement> findByWarehouseOrderByCreatedAtDesc(@Param("warehouse") Warehouse warehouse);

	@Query("SELECT m FROM StockMovement m WHERE m.movementType = :type AND m.stock.warehouse.org = :orgId ORDER BY m.createdAt DESC")
	List<StockMovement> findByMovementTypeAndOrg(@Param("type") MovementType type, @Param("orgId") Long orgId);

	@Query("SELECT m FROM StockMovement m WHERE m.referenceType = :refType AND m.referenceId = :refId")
	List<StockMovement> findByReference(@Param("refType") String refType, @Param("refId") Long refId);

	@Query("SELECT m FROM StockMovement m WHERE m.batchNumber = :batchNumber AND m.stock.warehouse.org = :orgId")
	List<StockMovement> findByBatchNumberAndOrg(@Param("batchNumber") String batchNumber, @Param("orgId") Long orgId);

	@Query("SELECT m FROM StockMovement m WHERE m.expiryDate IS NOT NULL AND m.expiryDate <= :date AND m.stock.warehouse.org = :orgId")
	List<StockMovement> findExpiringBeforeDate(@Param("date") Timestamp date, @Param("orgId") Long orgId);

	@Query("SELECT m FROM StockMovement m WHERE m.createdAt BETWEEN :start AND :end AND m.stock.warehouse.org = :orgId ORDER BY m.createdAt DESC")
	List<StockMovement> findByDateRangeAndOrg(@Param("start") Timestamp start, @Param("end") Timestamp end,
			@Param("orgId") Long orgId);

	@Query("SELECT SUM(m.quantity) FROM StockMovement m WHERE m.stock = :stock AND m.movementType IN :inboundTypes")
	Double getTotalInboundQuantity(@Param("stock") Stock stock, @Param("inboundTypes") List<MovementType> inboundTypes);

	@Query("SELECT SUM(m.quantity) FROM StockMovement m WHERE m.stock = :stock AND m.movementType IN :outboundTypes")
	Double getTotalOutboundQuantity(@Param("stock") Stock stock,
			@Param("outboundTypes") List<MovementType> outboundTypes);

	@Query("SELECT m FROM StockMovement m WHERE m.stock.material = :material AND m.stock.warehouse.org = :orgId ORDER BY m.createdAt DESC")
	List<StockMovement> findByMaterialAndOrg(@Param("material") Material material, @Param("orgId") Long orgId);
}