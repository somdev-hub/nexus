package com.nexus.core.payload;

import com.nexus.core.entities.Material;

import lombok.Data;

@Data
public class MaterialRequirementDto {
    private Material material;

    private Double quantity;
}
