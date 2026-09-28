package com.nexus.core.repository;

import com.nexus.core.model.entities.QuotationLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuotationLineItemRepo extends JpaRepository<QuotationLineItem, Long> {
}
