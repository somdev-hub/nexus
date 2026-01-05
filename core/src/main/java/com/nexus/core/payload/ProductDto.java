package com.nexus.core.payload;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.nexus.core.entities.ProductCategory;
import com.nexus.core.entities.ProductStatus;

import lombok.Data;

@Data
public class ProductDto {
    private String name;

    private String code;

    private String description;

    private List<MultipartFile> productImages;

    private List<MaterialRequirementDto> materialRequirements;

    private Long org;

    private Long productManager;

    private Double sellingPrice;

    private Double cost;

    private Boolean taxCharged;

    private Double taxPercentage;

    private ProductStatus productStatus;

    private ProductCategory productCategory;

    private Double price;
}
