package com.nexus.core.entities;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_supplier_quotations", schema = "core")
public class SupplierQuotation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quotation_id")
    private Long quotationId;

    @Column(name = "quotation_number", unique = true)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account supplierOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account buyerOrg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Column(name = "valid_from")
    private Date validFrom;

    @Column(name = "valid_to")
    private Date validTo;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "parent_quotation_id")
    private Long parentQuotationId;

    @Column(name = "version_number")
    private Integer versionNumber = 1;

    @Column(name = "converted_to_po_id")
    private Long convertedToPoId;

    @Version
    private Long version = 0L;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<QuotationLineItem> lineItems = new ArrayList<>();

    public enum QuotationStatus {
        DRAFT,
        SENT,
        ACCEPTED,
        REJECTED,
        EXPIRED,
        CONVERTED
    }

    public void addLineItem(QuotationLineItem item) {
        lineItems.add(item);
        item.setQuotation(this);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = lineItems.stream()
                .map(li -> li.getTotalPrice() != null ? li.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
