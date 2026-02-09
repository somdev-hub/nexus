package com.nexus.hr.repository;

import com.nexus.hr.model.entities.EmployeeLeaveAllocation;
import com.nexus.hr.model.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeLeaveAllocationRepo extends JpaRepository<EmployeeLeaveAllocation, Long> {

    /**
     * Find leave allocation for an employee by hrId, leaveType, and year
     */
    Optional<EmployeeLeaveAllocation> findByHrEntity_HrIdAndLeaveTypeAndYear(Long hrId, LeaveType leaveType, Integer year);
}

