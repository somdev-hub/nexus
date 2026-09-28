package com.nexus.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.Account;
public interface AccountRepository extends JpaRepository<Account, Long> {
}