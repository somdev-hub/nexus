package com.nexus.core.service;

import com.nexus.core.entities.Account;

public interface AccountDirectory {

    /**
     * Returns the core Account row for an IAM organization, creating a stub row
     * on first use. Core Account rows are not auto-created at signup, so every
     * org-scoped write must go through here instead of a bare find-or-throw.
     */
    Account getOrCreateAccount(Long orgId);
}
