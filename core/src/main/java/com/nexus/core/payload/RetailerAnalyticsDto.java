package com.nexus.core.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class RetailerAnalyticsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardDto {
        private BigDecimal totalSpend;
        private long openPoCount;
        private long inboundShipmentCount;
        private double otifPercentage;
        private double avgSupplierPerformance;
        private long totalSuppliers;
        private long activePartnerships;
        private double inventoryValue;
        private List<Map<String, Object>> spendTrend;
        private List<Map<String, Object>> bottleneckAlerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpendAnalyticsDto {
        private Map<String, BigDecimal> spendBySupplier;
        private Map<String, BigDecimal> spendByCategory;
        private Map<String, BigDecimal> spendByMonth;
        private BigDecimal totalSpend;
        private BigDecimal avgOrderValue;
        private long totalOrders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplyChainVisibilityDto {
        private String purchaseOrderNumber;
        private String poStatus;
        private String shipmentNumber;
        private String shipmentStatus;
        private List<Map<String, Object>> trackingTimeline;
        private String goodsReceiptNumber;
        private String invoiceNumber;
        private String invoiceStatus;
        private String freightInvoiceNumber;
        private List<Map<String, Object>> milestones;
        private List<Map<String, Object>> bottlenecks;
        private double totalLeadTimeDays;
    }
}
