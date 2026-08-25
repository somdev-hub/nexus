package com.nexus.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
}