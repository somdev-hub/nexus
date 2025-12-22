package com.nexus.core.entities;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "t_materials", schema = "core")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    private Long org;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    private Double pricePerUnit;

    private String unit;

    private Double productionCostPerUnit;

    private Double productionCapacityPerMonth;

    private Double availableQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @OneToMany(mappedBy = "material")
    private List<MaterialRequirement> materialRequirements;

    @OneToMany(mappedBy = "material")
    private List<BuyerOrgMaterialAvailability> buyerAvailabilities;

    @OneToMany(mappedBy = "material")
    private List<Order> orders;
}
