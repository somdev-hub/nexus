package com.nexus.core.repository;

import com.nexus.core.entities.QuotationLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuotationLineItemRepo extends JpaRepository<QuotationLineItem, Long> {
}
