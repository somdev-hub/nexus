package com.nexus.core.payload;

import lombok.Data;

@Data
public class MaterialRequirementDto {
    // Preferred: reference to an existing received Material.
    private Long materialId;

    // Legacy free-text material name (kept for backward compatibility).
    private String material;

    private Double quantity;
}
