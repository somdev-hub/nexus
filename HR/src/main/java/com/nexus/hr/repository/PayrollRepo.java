package com.nexus.hr.repository;

import com.nexus.hr.model.entities.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepo extends JpaRepository<Payroll, Long> {
    @Query("SELECT p FROM Payroll p WHERE p.compensation.compensationId = :compensationId " +
           "AND p.month = :month AND p.year = :year order by p.paidOn desc")
    List<Payroll> findByCompensationIdAndMonthAndYear(
            @Param("compensationId") Long compensationId,
            @Param("month") String month,
            @Param("year") Integer year
    );
}
