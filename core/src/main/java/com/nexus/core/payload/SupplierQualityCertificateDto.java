package com.nexus.core.payload;

import com.nexus.core.entities.SupplierQualityCertificate;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierQualityCertificateDto {

    private Long certificateId;

    private Long purchaseOrderId;
    private String poNumber;

    private Long shipmentId;
    private String shipmentNumber;

    private Long catalogId;
    private String catalogName;

    @NotNull(message = "Certificate type is required")
    private SupplierQualityCertificate.CertificateType certificateType;

    private String certificateNumber;

    private String dmsDocumentId;

    private String dmsDocumentUrl;

    private Date issuedDate;

    private Date expiryDate;

    private String notes;

    private Long supplierOrgId;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
