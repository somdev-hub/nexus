package com.nexus.core.entities;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "t_warehouses", schema = "core")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private Long warehouseManager;

    private Long org;

    private String location;

    private Double storageCapacity;

    private Double currentUtilization;

    @OneToMany(mappedBy = "warehouse")
    private List<Material> materials;

}
