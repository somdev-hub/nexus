package com.nexus.iam.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @JsonManagedReference(value = "organization-users")
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "organization")
    @JsonManagedReference(value = "organization-documents")
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "organization")
    @JsonManagedReference(value = "organization-departments")
    private List<Department> departments = new ArrayList<>();

    private Timestamp createdAt;
}
