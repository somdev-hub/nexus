package com.nexus.iam.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "organization")
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "organization")
    private List<Department> departments = new ArrayList<>();

    private Timestamp createdAt;
}
