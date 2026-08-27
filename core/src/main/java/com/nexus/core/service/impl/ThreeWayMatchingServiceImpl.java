package com.nexus.core.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.entities.GoodsReceipt;
import com.nexus.core.entities.GoodsReceiptStatus;
import com.nexus.core.entities.Invoice;
import com.nexus.core.entities.InvoiceStatus;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderLineItem;
import com.nexus.core.repository.GoodsReceiptRepo;
import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.PurchaseOrderLineItemRepo;
import com.nexus.core.service.ThreeWayMatchingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreeWayMatchingServiceImpl implements ThreeWayMatchingService {

	private final GoodsReceiptRepo goodsReceiptRepo;
	private final InvoiceRepo invoiceRepo;
	private final PurchaseOrderLineItemRepo poLineItemRepo;

	private static final BigDecimal TOLERANCE = new BigDecimal("0.01"); // 1 cent tolerance

	@Override
	public MatchingResult performThreeWayMatch(PurchaseOrder purchaseOrder) {
		List<String> discrepancies = new ArrayList<>();

		// Get PO line items
		List<PurchaseOrderLineItem> poLineItems = poLineItemRepo
				.findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());

		// Calculate PO total
		BigDecimal poTotal = poLineItems.stream()
				.map(item -> item.getTotalPrice() != null ? BigDecimal.valueOf(item.getTotalPrice()) : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Get received goods receipts (only RECEIVED status)
		List<GoodsReceipt> goodsReceipts = goodsReceiptRepo.findByPurchaseOrderAndStatus(
				purchaseOrder.getPurchaseOrderId(), GoodsReceiptStatus.RECEIVED, null).getContent();

		// Calculate GR total
		BigDecimal grTotal = goodsReceipts.stream()
				.flatMap(gr -> gr.getLineItems().stream())
				.map(item -> {
					BigDecimal qty = item.getQuantityAccepted() != null ? item.getQuantityAccepted() : BigDecimal.ZERO;
					BigDecimal price = item.getPoLineItem() != null && item.getPoLineItem().getUnitPrice() != null
							? BigDecimal.valueOf(item.getPoLineItem().getUnitPrice())
							: BigDecimal.ZERO;
					return qty.multiply(price);
				})
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Get approved invoices
		List<Invoice> invoices = invoiceRepo.findByPurchaseOrderAndStatus(
				purchaseOrder.getPurchaseOrderId(), InvoiceStatus.APPROVED, null).getContent();

		// Calculate invoice total
		BigDecimal invoiceTotal = invoices.stream()
				.flatMap(inv -> inv.getLineItems().stream())
				.map(item -> item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Compare PO vs GR
		if (poTotal.compareTo(grTotal) != 0 && poTotal.subtract(grTotal).abs().compareTo(TOLERANCE) > 0) {
			discrepancies.add(String.format("PO total (%s) does not match GR total (%s)", poTotal, grTotal));
		}

		// Compare GR vs Invoice
		if (grTotal.compareTo(invoiceTotal) != 0 && grTotal.subtract(invoiceTotal).abs().compareTo(TOLERANCE) > 0) {
			discrepancies.add(String.format("GR total (%s) does not match Invoice total (%s)", grTotal, invoiceTotal));
		}

		// Compare PO vs Invoice
		if (poTotal.compareTo(invoiceTotal) != 0 && poTotal.subtract(invoiceTotal).abs().compareTo(TOLERANCE) > 0) {
			discrepancies.add(String.format("PO total (%s) does not match Invoice total (%s)", poTotal, invoiceTotal));
		}

		// Line-level matching
		Map<Long, BigDecimal> poLineMap = poLineItems.stream()
				.collect(Collectors.toMap(
						PurchaseOrderLineItem::getLineItemId,
						item -> item.getTotalPrice() != null ? BigDecimal.valueOf(item.getTotalPrice())
								: BigDecimal.ZERO));

		Map<Long, BigDecimal> grLineMap = goodsReceipts.stream()
				.flatMap(gr -> gr.getLineItems().stream())
				.collect(Collectors.groupingBy(
						item -> item.getPoLineItem() != null ? item.getPoLineItem().getLineItemId() : -1L,
						Collectors.reducing(BigDecimal.ZERO,
								item -> {
									BigDecimal qty = item.getQuantityAccepted() != null ? item.getQuantityAccepted()
											: BigDecimal.ZERO;
									BigDecimal price = item.getPoLineItem() != null
											&& item.getPoLineItem().getUnitPrice() != null
													? BigDecimal.valueOf(item.getPoLineItem().getUnitPrice())
													: BigDecimal.ZERO;
									return qty.multiply(price);
								},
								BigDecimal::add)));

		Map<Long, BigDecimal> invoiceLineMap = invoices.stream()
				.flatMap(inv -> inv.getLineItems().stream())
				.collect(Collectors.groupingBy(
						item -> item.getPoLineItem() != null ? item.getPoLineItem().getLineItemId() : -1L,
						Collectors.reducing(BigDecimal.ZERO,
								item -> item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO,
								BigDecimal::add)));

		// Check each PO line
		for (Map.Entry<Long, BigDecimal> entry : poLineMap.entrySet()) {
			Long lineId = entry.getKey();
			BigDecimal poLineTotal = entry.getValue();
			BigDecimal grLineTotal = grLineMap.getOrDefault(lineId, BigDecimal.ZERO);
			BigDecimal invoiceLineTotal = invoiceLineMap.getOrDefault(lineId, BigDecimal.ZERO);

			if (poLineTotal.compareTo(grLineTotal) != 0
					&& poLineTotal.subtract(grLineTotal).abs().compareTo(TOLERANCE) > 0) {
				discrepancies.add(
						String.format("Line %d: PO amount (%s) != GR amount (%s)", lineId, poLineTotal, grLineTotal));
			}
			if (grLineTotal.compareTo(invoiceLineTotal) != 0
					&& grLineTotal.subtract(invoiceLineTotal).abs().compareTo(TOLERANCE) > 0) {
				discrepancies.add(String.format("Line %d: GR amount (%s) != Invoice amount (%s)", lineId, grLineTotal,
						invoiceLineTotal));
			}
		}

		boolean matched = discrepancies.isEmpty();
		String message = matched ? "Three-way match successful"
				: "Three-way match failed: " + String.join("; ", discrepancies);

		return new MatchingResult(matched, message, discrepancies, poTotal, grTotal, invoiceTotal);
	}

	@Override
	public boolean canInvoice(Long purchaseOrderId) {
		List<GoodsReceipt> goodsReceipts = goodsReceiptRepo.findByPurchaseOrderAndStatus(
				purchaseOrderId, GoodsReceiptStatus.RECEIVED, null).getContent();

		return !goodsReceipts.isEmpty() && goodsReceipts.stream()
				.anyMatch(gr -> gr.getLineItems().stream()
						.anyMatch(item -> item.getQuantityAccepted() != null
								&& item.getQuantityAccepted().compareTo(BigDecimal.ZERO) > 0));
	}

	@Override
	public MatchingResult validateInvoiceMatch(Invoice invoice) {
		PurchaseOrder po = invoice.getPurchaseOrder();
		if (po == null) {
			return new MatchingResult(false, "Invoice not linked to a purchase order",
					List.of("Missing purchase order reference"),
					BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		}
		return performThreeWayMatch(po);
	}

	@Override
	public Map<String, Object> getMatchingSummary(Long purchaseOrderId) {
		Map<String, Object> summary = new HashMap<>();

		// Get PO
		// Note: In real implementation, you'd fetch the PO from repository
		// For now, return basic structure
		summary.put("purchaseOrderId", purchaseOrderId);
		summary.put("canInvoice", canInvoice(purchaseOrderId));

		// Get GRs
		List<GoodsReceipt> goodsReceipts = goodsReceiptRepo.findByPurchaseOrderAndStatus(
				purchaseOrderId, GoodsReceiptStatus.RECEIVED, null).getContent();
		summary.put("goodsReceiptCount", goodsReceipts.size());
		summary.put("totalReceivedQty", goodsReceipts.stream()
				.flatMap(gr -> gr.getLineItems().stream())
				.map(item -> item.getQuantityAccepted() != null ? item.getQuantityAccepted() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add));

		// Get Invoices
		List<Invoice> invoices = invoiceRepo.findByPurchaseOrderAndStatus(
				purchaseOrderId, InvoiceStatus.APPROVED, null).getContent();
		summary.put("invoiceCount", invoices.size());
		summary.put("totalInvoicedAmount", invoices.stream()
				.flatMap(inv -> inv.getLineItems().stream())
				.map(item -> item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add));

		return summary;
	}
}