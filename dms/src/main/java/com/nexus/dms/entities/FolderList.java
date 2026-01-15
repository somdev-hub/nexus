package com.nexus.dms.entities;

import java.sql.Timestamp;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "t_dms_folder_lists", schema = "dms")
@Data
public class FolderList {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String folderName;

    private String region;

    private String createdBy;

    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    private OrgType orgType;

    @OneToMany(mappedBy = "folderList", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<DocumentRecord> documentRecords;

    public FolderList(String folderName, String region, String createdBy, OrgType orgType) {
        this.folderName = folderName;
        this.region = region;
        this.createdBy = createdBy;
        this.orgType = orgType;
    }

    public FolderList() {
    }
}
