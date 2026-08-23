package com.nexus.iam.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "t_organizations", schema = "iam")
@Data
@lombok.ToString(exclude = {"users", "documents", "departments"})
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orgName;

    @Enumerated(value = EnumType.STRING)
    private OrgType orgType;

    private Double trustScore;

    private String orgEmail;

    private String orgPhone;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String pinCode;

    private String country;

    @OneToMany(mappedBy = "organization")
    @JsonManagedReference(value = "organization-users")
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "organization")
    @JsonManagedReference(value = "organization-documents")
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "organization")
    @JsonManagedReference(value = "organization-departments")
    private List<Department> departments = new ArrayList<>();

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    private void onCreate() {
        isActive = true;
    }


}
