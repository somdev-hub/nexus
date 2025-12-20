package com.nexus.iam.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_people", schema = "iam")
@Data
public class People {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User user;

    @OneToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne
    private Organization organization;
}
