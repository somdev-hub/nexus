package com.nexus.iam.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Lightweight UserDetails implementation to prevent circular reference issues
 * in authentication tokens. Only contains essential authentication data.
 */
public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Set<GrantedAuthority> authorities;
    private final Boolean enabled;
    private final Boolean accountNonExpired;
    private final Boolean accountNonLocked;
    private final Boolean credentialsNonExpired;

    public CustomUserPrincipal(Long id, String email, String password,
                              Set<GrantedAuthority> authorities,
                              Boolean enabled, Boolean accountNonExpired,
                              Boolean accountNonLocked, Boolean credentialsNonExpired) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities != null ? authorities : new HashSet<>();
        this.enabled = enabled != null ? enabled : true;
        this.accountNonExpired = accountNonExpired != null ? accountNonExpired : true;
        this.accountNonLocked = accountNonLocked != null ? accountNonLocked : true;
        this.credentialsNonExpired = credentialsNonExpired != null ? credentialsNonExpired : true;
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
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
}
