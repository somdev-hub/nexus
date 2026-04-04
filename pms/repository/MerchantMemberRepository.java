package com.nexus.pms.repository;

import com.nexus.pms.model.entities.MerchantMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MerchantMember entity.
 * Provides CRUD operations for merchant member/employee management.
 */
@Repository
public interface MerchantMemberRepository extends JpaRepository<MerchantMember, Long> {

    /**
     * Find merchant member by merchant ID and source member ID.
     */
    @Query("SELECT mm FROM MerchantMember mm WHERE mm.merchant.merchantId = :merchantId AND mm.sourceMemberId = :sourceMemberId")
    Optional<MerchantMember> findByMerchantAndSourceId(@Param("merchantId") Long merchantId,
            @Param("sourceMemberId") Long sourceMemberId);

    /**
     * Get all active members for a merchant.
     */
    @Query("SELECT mm FROM MerchantMember mm WHERE mm.merchant.merchantId = :merchantId AND mm.isActive = true")
    List<MerchantMember> findActiveByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * Get eligible members for payment.
     */
    @Query("SELECT mm FROM MerchantMember mm " +
            "WHERE mm.merchant.merchantId = :merchantId " +
            "AND mm.isActive = true " +
            "AND mm.isEligibleForPayment = true " +
            "AND mm.bankAccountNumber IS NOT NULL")
    List<MerchantMember> findEligibleForPayment(@Param("merchantId") Long merchantId);

    /**
     * Find member and verify bank details are present.
     */
    @Query("SELECT mm FROM MerchantMember mm WHERE mm.merchantMemberId = :memberId AND mm.bankAccountNumber IS NOT NULL")
    Optional<MerchantMember> findWithBankDetails(@Param("memberId") Long memberId);
}
