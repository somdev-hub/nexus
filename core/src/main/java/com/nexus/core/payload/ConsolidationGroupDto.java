package com.nexus.core.payload;

import com.nexus.core.entities.ConsolidationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsolidationGroupDto {
    private Long groupId;
    private String groupNumber;
    private ConsolidationStatus status;
    private List<Long> shipmentIds;
    private Double totalWeight;
    private Double totalVolume;
    private Double utilizationPct;
    private String notes;
}
