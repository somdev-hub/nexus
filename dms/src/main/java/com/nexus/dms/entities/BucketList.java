package com.nexus.dms.entities;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_dms_bucket_lists", schema = "dms")
@Data
public class BucketList {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String bucketName;

    private String region;

    private String createdBy;

    private String createdAt;

    private OrgType orgType;

    @OneToMany(mappedBy = "bucketList")
    private List<DocumentRecord> documentRecords;
}
