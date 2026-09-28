package com.nexus.core.service.implementations;

import com.nexus.core.model.entities.Account;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.service.interfaces.AccountDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDirectoryImpl implements AccountDirectory {

    private final AccountRepository accountRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Account getOrCreateAccount(Long orgId) {
        return accountRepository.findById(orgId).orElseGet(() -> {
            log.info("Provisioning core Account row for org {}", orgId);
            // Explicit id keeps core Account rows aligned with IAM organization ids.
            // ON CONFLICT covers races; setval keeps the identity sequence ahead.
            jdbcTemplate.execute("INSERT INTO core.t_accounts (account_id, created_at, updated_at, is_active)"
                    + " VALUES (" + orgId + ", now(), now(), true)"
                    + " ON CONFLICT (account_id) DO NOTHING");
            jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('core.t_accounts','account_id'),"
                    + " (SELECT max(account_id) FROM core.t_accounts))");
            return accountRepository.findById(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        });
    }
}
