package com.nexus.core.service.implementations;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.model.entities.Material;
import com.nexus.core.model.entities.Product;
import com.nexus.core.model.entities.ProductionOrder;
import com.nexus.core.model.entities.ProductionOrderMaterial;
import com.nexus.core.model.entities.Stock;
import com.nexus.core.model.entities.StockMovement;
import com.nexus.core.model.entities.Warehouse;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.ProductRepo;
import com.nexus.core.repository.ProductionOrderRepo;
import com.nexus.core.repository.StockRepo;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.security.OrganizationContextHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionServiceImpl implements com.nexus.core.service.interfaces.ProductionService {

	private final ProductRepo productRepo;
	private final MaterialRepo materialRepo;
	private final StockRepo stockRepo;
	private final WarehouseRepo warehouseRepo;
	private final ProductionOrderRepo productionOrderRepo;

	@Override
	@Transactional
	public ResponseEntity<?> createProductionOrder(Long productId, Double quantity, Long warehouseId, String notes, Timestamp deadline) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Product product = productRepo.findByProductIdAndOrg(productId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
		if (quantity == null || quantity <= 0) throw new ValidationException("Quantity must be positive");

		Warehouse warehouse = warehouseId != null
				? warehouseRepo.findByWarehouseIdAndOrg(warehouseId, orgId).orElse(null)
				: findProductionWarehouse(orgId);
		if (warehouse == null) throw new ValidationException("Production warehouse not found");

		ProductionOrder po = new ProductionOrder();
		po.setProduct(product);
		po.setQuantity(quantity);
		po.setWarehouse(warehouse);
		po.setOrg(orgId);
		po.setNotes(notes);
		po.setDeadline(deadline);
		po.setStatus(ProductionOrder.ProductionStatus.PLANNED);
		return ResponseEntity.ok(productionOrderRepo.save(po));
	}

	@Override
	public ResponseEntity<?> getProductionOrders(Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return ResponseEntity.ok(productionOrderRepo.findByOrg(orgId, pageable));
	}

	@Override
	public ResponseEntity<?> getProductionOrder(Long id) {
		ProductionOrder po = productionOrderRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("ProductionOrder", "id", id));
		return ResponseEntity.ok(po);
	}

	@Override
	@Transactional
	public ResponseEntity<?> allocate(Long productionOrderId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		ProductionOrder po = productionOrderRepo.findById(productionOrderId)
				.orElseThrow(() -> new ResourceNotFoundException("ProductionOrder", "id", productionOrderId));
		if (po.getOrg() != null && !po.getOrg().equals(orgId)) throw new ValidationException("Not allowed");
		if (po.getStatus() != ProductionOrder.ProductionStatus.PLANNED) throw new ValidationException("Order not in PLANNED state");
		if (po.getMaterials() != null && !po.getMaterials().isEmpty()) throw new ValidationException("Already allocated");

		Product product = po.getProduct();
		if (product.getMaterialRequirements() == null || product.getMaterialRequirements().isEmpty())
			throw new ValidationException("Product has no bill of materials");

		double qty = po.getQuantity();
		// validate & allocate each material
		for (var mr : product.getMaterialRequirements()) {
			Material material = mr.getMaterial();
			double required = (mr.getQuantity() != null ? mr.getQuantity() : 0.0) * qty;
			if (required <= 0) continue;

			// find available stock across warehouses
			List<Stock> stocks = stockRepo.findActiveByMaterial(material);
			double available = stocks.stream().mapToDouble(s -> s.getQuantityAvailable() != null ? s.getQuantityAvailable() : 0.0).sum();
			if (available < required) {
				throw new ValidationException("Insufficient stock for material " + material.getCode() + " (need " + required + ", have " + available + ")");
			}

			// allocate from stocks (first available)
			double remaining = required;
			for (Stock s : stocks) {
				if (remaining <= 0) break;
				double avail = s.getQuantityAvailable() != null ? s.getQuantityAvailable() : 0.0;
				if (avail <= 0) continue;
				double take = Math.min(avail, remaining);
				s.setQuantityReserved((s.getQuantityReserved() != null ? s.getQuantityReserved() : 0.0) + take);
				s.recalculateAvailable();
				stockRepo.save(s);

				ProductionOrderMaterial pom = new ProductionOrderMaterial();
				pom.setProductionOrder(po);
				pom.setMaterial(material);
				pom.setWarehouse(s.getWarehouse());
				pom.setRequiredQty(required);
				pom.setAllocatedQty(take);
				po.getMaterials().add(pom);
				remaining -= take;
			}
		}
		productionOrderRepo.save(po);
		return ResponseEntity.ok(po);
	}

	@Override
	@Transactional
	public ResponseEntity<?> startProduction(Long productionOrderId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		ProductionOrder po = productionOrderRepo.findById(productionOrderId)
				.orElseThrow(() -> new ResourceNotFoundException("ProductionOrder", "id", productionOrderId));
		if (po.getOrg() != null && !po.getOrg().equals(orgId)) throw new ValidationException("Not allowed");
		if (po.getStatus() != ProductionOrder.ProductionStatus.PLANNED) throw new ValidationException("Order not in PLANNED state");
		if (po.getMaterials() == null || po.getMaterials().isEmpty()) throw new ValidationException("Materials not allocated");

		// issue stock
		for (ProductionOrderMaterial pom : po.getMaterials()) {
			Material material = pom.getMaterial();
			Stock stock = stockRepo.findByMaterialAndWarehouse(material, pom.getWarehouse()).orElse(null);
			if (stock != null) {
				double alloc = pom.getAllocatedQty() != null ? pom.getAllocatedQty() : 0.0;
				stock.setQuantityOnHand(Math.max(0, (stock.getQuantityOnHand() != null ? stock.getQuantityOnHand() : 0.0) - alloc));
				// release reservation too
				stock.setQuantityReserved(Math.max(0, (stock.getQuantityReserved() != null ? stock.getQuantityReserved() : 0.0) - alloc));
				stock.recalculateAvailable();
				stockRepo.save(stock);
			}
		}
		po.setStatus(ProductionOrder.ProductionStatus.IN_PROGRESS);
		po.setStartedAt(Timestamp.valueOf(LocalDateTime.now()));
		return ResponseEntity.ok(productionOrderRepo.save(po));
	}

	@Override
	@Transactional
	public ResponseEntity<?> completeProduction(Long productionOrderId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		ProductionOrder po = productionOrderRepo.findById(productionOrderId)
				.orElseThrow(() -> new ResourceNotFoundException("ProductionOrder", "id", productionOrderId));
		if (po.getOrg() != null && !po.getOrg().equals(orgId)) throw new ValidationException("Not allowed");
		if (po.getStatus() != ProductionOrder.ProductionStatus.IN_PROGRESS) throw new ValidationException("Order not in progress");

		double revenue = po.getQuantity() * (po.getProduct().getSellingPrice() != null ? po.getProduct().getSellingPrice() : 0.0);
		po.setRevenueAmount(revenue);
		po.setStatus(ProductionOrder.ProductionStatus.COMPLETED);
		po.setCompletedAt(Timestamp.valueOf(LocalDateTime.now()));
		return ResponseEntity.ok(productionOrderRepo.save(po));
	}

	@Override
	@Transactional
	public ResponseEntity<?> cancelProduction(Long productionOrderId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		ProductionOrder po = productionOrderRepo.findById(productionOrderId)
				.orElseThrow(() -> new ResourceNotFoundException("ProductionOrder", "id", productionOrderId));
		if (po.getOrg() != null && !po.getOrg().equals(orgId)) throw new ValidationException("Not allowed");
		if (po.getStatus() == ProductionOrder.ProductionStatus.COMPLETED) throw new ValidationException("Cannot cancel completed order");
		// release reservations if planned/allocated
		if (po.getMaterials() != null) {
			for (ProductionOrderMaterial pom : po.getMaterials()) {
				Material material = pom.getMaterial();
				Stock stock = stockRepo.findByMaterialAndWarehouse(material, pom.getWarehouse()).orElse(null);
				if (stock != null) {
					double alloc = pom.getAllocatedQty() != null ? pom.getAllocatedQty() : 0.0;
					stock.setQuantityReserved(Math.max(0, (stock.getQuantityReserved() != null ? stock.getQuantityReserved() : 0.0) - alloc));
					stock.recalculateAvailable();
					stockRepo.save(stock);
				}
			}
		}
		po.setStatus(ProductionOrder.ProductionStatus.CANCELLED);
		return ResponseEntity.ok(productionOrderRepo.save(po));
	}

	@Override
	@Transactional
	public ResponseEntity<?> releaseBatch(Long stockId) {
		Stock stock = stockRepo.findById(stockId).orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", stockId));
		double qty = stock.getQuantityOnHand() != null ? stock.getQuantityOnHand() : 0.0;
		if (qty <= 0) return ResponseEntity.ok("Nothing to release");
		// return to reserve warehouses in reverse order C->B->A
		Long orgId = stock.getMaterial().getOrg();
		List<Warehouse> reserves = warehouseRepo.findByOrg(orgId, org.springframework.data.domain.PageRequest.of(0, 10)).getContent().stream()
				.filter(w -> "RESERVE".equalsIgnoreCase(w.getWarehouseType()))
				.sorted((a, b) -> Double.compare(b.getCapacityMultiplier() != null ? b.getCapacityMultiplier() : 0, a.getCapacityMultiplier() != null ? a.getCapacityMultiplier() : 0))
				.toList();
		double remaining = qty;
		for (Warehouse w : reserves) {
			if (remaining <= 0) break;
			Stock target = stockRepo.findByMaterialAndWarehouse(stock.getMaterial(), w).orElse(null);
			if (target == null) {
				target = new Stock();
				target.setMaterial(stock.getMaterial());
				target.setWarehouse(w);
				target.setQuantityOnHand(0.0);
				target.setQuantityReserved(0.0);
				target.setQuantityAvailable(0.0);
			}
			target.setQuantityOnHand((target.getQuantityOnHand() != null ? target.getQuantityOnHand() : 0.0) + remaining);
			target.recalculateAvailable();
			stockRepo.save(target);
			remaining = 0;
		}
		stock.setQuantityOnHand(0.0);
		stock.recalculateAvailable();
		stockRepo.save(stock);
		return ResponseEntity.ok("Released");
	}

	@Scheduled(cron = "0 0 0 * * *")
	@Transactional
	public void autoReleaseExpiredBatches() {
		java.time.LocalDateTime now = java.time.LocalDateTime.now();
		for (ProductionOrder po : productionOrderRepo.findAll()) {
			if (po.getDeadline() == null) continue;
			if (po.getDeadline().toLocalDateTime().isAfter(now)) continue;
			if (po.getStatus() == ProductionOrder.ProductionStatus.COMPLETED || po.getStatus() == ProductionOrder.ProductionStatus.CANCELLED) continue;
			if (po.getMaterials() == null) continue;
			for (ProductionOrderMaterial pom : po.getMaterials()) {
				try {
					Stock stock = stockRepo.findByMaterialAndWarehouse(pom.getMaterial(), pom.getWarehouse()).orElse(null);
					if (stock != null && stock.getQuantityOnHand() != null && stock.getQuantityOnHand() > 0) {
						releaseBatch(stock.getStockId());
					}
				} catch (Exception e) {
					log.error("Auto-release failed for production order {}", po.getProductionOrderId(), e);
				}
			}
		}
	}

	private Warehouse findProductionWarehouse(Long orgId) {
		return warehouseRepo.findByOrg(orgId, org.springframework.data.domain.PageRequest.of(0, 10)).getContent().stream()
				.filter(w -> "PRODUCTION".equalsIgnoreCase(w.getWarehouseType()))
				.findFirst()
				.orElseGet(() -> warehouseRepo.findByOrg(orgId, org.springframework.data.domain.PageRequest.of(0, 1)).getContent().stream().findFirst().orElse(null));
	}
}