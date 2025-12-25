package com.nexus.iam.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
@Table(name = "t_organizations", schema = "iam")
@Data
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orgName;

    @Enumerated(value = EnumType.STRING)
    private OrgType orgType;

    private Double trustScore;

    @OneToMany(mappedBy = "organization")
    private List<People> people = new ArrayList<>();

    @OneToMany(mappedBy = "organization")
    private List<Document> documents = new ArrayList<>();

    private Timestamp createdAt;
}
