package com.nexus.iam.service.impl;

import com.nexus.iam.entities.User;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.security.CustomUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + email));
        return buildCustomUserPrincipal(user);
    }

    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        return buildCustomUserPrincipal(user);
    }

    /**
     * Builds a lightweight CustomUserPrincipal from User entity to prevent
     * circular reference issues in authentication tokens
     */
    private CustomUserPrincipal buildCustomUserPrincipal(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            user.getRoles().forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()))
            );
        }

        return new CustomUserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            authorities,
            user.getEnabled(),
            user.getAccountNonExpired(),
            user.getAccountNonLocked(),
            user.getCredentialsNonExpired()
        );
    }
}

