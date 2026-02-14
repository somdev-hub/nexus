package com.nexus.iam.entities;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "t_users", schema = "iam")
@NoArgsConstructor
@lombok.ToString(exclude = {"headedDepartments", "memberOfDepartments", "organization", "roles"})
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(unique = true)
    private String phone;

    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Timestamp createdAt;

    private String profilePhoto;

    private String personalEmail;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer age;

    private Date dateOfBirth;

    private Boolean enabled = true;

    private Boolean accountNonExpired = true;

    private Boolean accountNonLocked = true;

    private Boolean credentialsNonExpired = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "t_user_roles", schema = "iam", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    @JsonBackReference(value = "organization-users")
    private Organization organization;

    @OneToMany(mappedBy = "departmentHead", fetch = FetchType.LAZY, cascade = {CascadeType.DETACH, CascadeType.REFRESH})
    @JsonBackReference(value = "department-head")
    private List<Department> headedDepartments = new java.util.ArrayList<>();

    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    @JsonBackReference(value = "department-members")
    private List<Department> memberOfDepartments = new java.util.ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
        }
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}
