package com.nexus.core.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.nexus.core.entities.GoodsReceipt;
import com.nexus.core.entities.Invoice;
import com.nexus.core.entities.PurchaseOrder;

public interface ThreeWayMatchingService {

	/**
	 * Perform three-way matching for a purchase order
	 * 
	 * @param purchaseOrder The purchase order to match
	 * @return Matching result with details
	 */
	MatchingResult performThreeWayMatch(PurchaseOrder purchaseOrder);

	/**
	 * Check if a PO can be invoiced (has received goods)
	 * 
	 * @param purchaseOrderId The purchase order ID
	 * @return true if goods have been received
	 */
	boolean canInvoice(Long purchaseOrderId);

	/**
	 * Check if an invoice matches the PO and GR
	 * 
	 * @param invoice The invoice to validate
	 * @return Matching result
	 */
	MatchingResult validateInvoiceMatch(Invoice invoice);

	/**
	 * Get matching summary for a PO
	 * 
	 * @param purchaseOrderId The purchase order ID
	 * @return Map with matching details
	 */
	Map<String, Object> getMatchingSummary(Long purchaseOrderId);

	/**
	 * Match result class
	 */
	class MatchingResult {
		private boolean matched;
		private String message;
		private List<String> discrepancies;
		private BigDecimal poTotal;
		private BigDecimal grTotal;
		private BigDecimal invoiceTotal;

		public MatchingResult(boolean matched, String message, List<String> discrepancies,
				BigDecimal poTotal, BigDecimal grTotal, BigDecimal invoiceTotal) {
			this.matched = matched;
			this.message = message;
			this.discrepancies = discrepancies;
			this.poTotal = poTotal;
			this.grTotal = grTotal;
			this.invoiceTotal = invoiceTotal;
		}

		public boolean isMatched() {
			return matched;
		}

		public void setMatched(boolean matched) {
			this.matched = matched;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public List<String> getDiscrepancies() {
			return discrepancies;
		}

		public void setDiscrepancies(List<String> discrepancies) {
			this.discrepancies = discrepancies;
		}

		public BigDecimal getPoTotal() {
			return poTotal;
		}

		public void setPoTotal(BigDecimal poTotal) {
			this.poTotal = poTotal;
		}

		public BigDecimal getGrTotal() {
			return grTotal;
		}

		public void setGrTotal(BigDecimal grTotal) {
			this.grTotal = grTotal;
		}

		public BigDecimal getInvoiceTotal() {
			return invoiceTotal;
		}

		public void setInvoiceTotal(BigDecimal invoiceTotal) {
			this.invoiceTotal = invoiceTotal;
		}
	}
}