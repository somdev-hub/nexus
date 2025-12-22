package com.nexus.core.entities;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "t_accounts", schema = "core")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long org;

    private String name;

    private Double currentAccountBalance;

    @OneToMany(mappedBy = "primaryOrg")
    private List<Partnership> primaryPartnerships;

    @OneToMany(mappedBy = "secondaryOrg")
    private List<Partnership> secondaryPartnerships;

}
