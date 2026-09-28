package com.nexus.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.InvoiceLineItem;
public interface InvoiceLineItemRepo extends JpaRepository<InvoiceLineItem, Long> {
}