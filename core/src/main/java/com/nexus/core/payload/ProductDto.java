package com.nexus.core.payload;

import java.util.List;

import lombok.Data;

@Data
public class ProductDto {
    private String name;

    private String code;

    private List<MaterialRequirementDto> materialRequirements;

    private Long org;

    private Long productManager;

    private Double price;
}
