package com.nexus.core.entities;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "t_products", schema = "core")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    private List<String> productImages;

    @OneToMany(mappedBy = "product")
    private List<MaterialRequirement> materialRequirements;

    private Long org;

    private Long productManager;

    private Double price;

    private Double sellingPrice;

    private Double cost;

    private Boolean taxCharged;

    private Double taxPercentage;

    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;

    @Enumerated(EnumType.STRING)
    private ProductCategory productCategory;

    @OneToMany(mappedBy = "product")
    private List<Order> orders;

}
