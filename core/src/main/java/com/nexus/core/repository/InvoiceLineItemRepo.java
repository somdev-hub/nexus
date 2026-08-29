package com.nexus.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.InvoiceLineItem;

@Repository
public interface InvoiceLineItemRepo extends JpaRepository<InvoiceLineItem, Long> {
}