package com.nexus.core.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for ABC Analysis (FR-RET-012).
 * Classifies inventory by annual consumption value for prioritization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbcAnalysisDto {

    private List<AbcItemDto> items;

    private AbcSummaryDto summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbcItemDto {
        private Long stockId;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private Long warehouseId;
        private Double quantityOnHand;
        private Double unitCost;
        private Double annualValue;
        private Double cumulativePercentage;
        private String abcCategory; // A, B, C
        private Double velocityScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbcSummaryDto {
        private int totalItems;
        private long categoryACount;
        private long categoryBCount;
        private long categoryCCount;
        private double totalInventoryValue;
        private double categoryAValue;
        private double categoryBValue;
        private double categoryCValue;
        private double categoryAPercentage;
        private double categoryBPercentage;
        private double categoryCPercentage;
    }
}
