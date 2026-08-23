package com.nexus.nexusbuddy.repository;

import com.nexus.nexusbuddy.model.entities.ClientConfigAllowedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientConfigAllowedUserRepository extends JpaRepository<ClientConfigAllowedUser, Long> {

    List<ClientConfigAllowedUser> findByClientConfigClientConfigIdAndIsActiveTrue(Long clientConfigId);

    List<ClientConfigAllowedUser> findByDomainAndIsActiveTrue(String domain);
}