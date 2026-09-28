package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.Account;
public interface AccountRepo extends JpaRepository<Account, Long> {

	Optional<List<Account>> findByOrg(Long orgId);

	Optional<Account> findByAccountId(Long accountId);

	Optional<Account> findByAccountIdAndIsActiveTrue(Long accountId);

}