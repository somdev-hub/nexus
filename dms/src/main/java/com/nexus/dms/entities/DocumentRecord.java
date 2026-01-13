package com.nexus.dms.entities;

import java.sql.Timestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_dms_document_records", schema = "dms")
@Data
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String documentName;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private Long documentSize;

    private String storageLocation;

    private String uploadedBy;

    private Timestamp uploadedAt;

    private String status;

    private String mimeType;

    private String documentUrl;

    private String checksum;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private UploaderType uploaderType;

    private Long orgId;

    private String dmsId;

    private String remarks;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private FolderList folderList;
}
