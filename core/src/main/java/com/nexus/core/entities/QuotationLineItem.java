package com.nexus.core.entities;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_quotation_line_items", schema = "core")
public class QuotationLineItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", referencedColumnName = "quotation_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierQuotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", referencedColumnName = "catalog_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierCatalog catalog;

    @Column(name = "description")
    private String description;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 19, scale = 4)
    private BigDecimal totalPrice;

    @Column(name = "notes")
    private String notes;
}
