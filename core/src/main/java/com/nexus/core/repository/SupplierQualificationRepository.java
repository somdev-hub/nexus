package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.QualificationStatus;
import com.nexus.core.entities.Supplier;
import com.nexus.core.entities.SupplierQualification;

@Repository
public interface SupplierQualificationRepository extends JpaRepository<SupplierQualification, Long> {

	Page<SupplierQualification> findAll(Pageable pageable);

	Page<SupplierQualification> findByStatus(QualificationStatus status, Pageable pageable);

	Page<SupplierQualification> findBySupplier(Supplier supplier, Pageable pageable);

	Page<SupplierQualification> findByRetailerOrg(Account retailerOrg, Pageable pageable);

	Page<SupplierQualification> findBySupplierAndRetailerOrg(Supplier supplier, Account retailerOrg, Pageable pageable);

	@Query("SELECT sq FROM SupplierQualification sq WHERE sq.supplier = :supplier AND sq.retailerOrg = :retailerOrg AND sq.status = :status")
	SupplierQualification findBySupplierAndRetailerOrgAndStatus(@Param("supplier") Supplier supplier,
			@Param("retailerOrg") Account retailerOrg, @Param("status") QualificationStatus status);

	Optional<SupplierQualification> findByQualificationId(Long qualificationId);

	// New methods for organization-scoped queries
	Page<SupplierQualification> findByRetailerOrgAccountId(Long accountId, Pageable pageable);

	@Query("SELECT sq FROM SupplierQualification sq WHERE sq.status = :status AND sq.retailerOrg.accountId = :accountId")
	Page<SupplierQualification> findByStatusAndRetailerOrgAccountId(@Param("status") QualificationStatus status,
			@Param("accountId") Long accountId, Pageable pageable);
}