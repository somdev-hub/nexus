package com.nexus.hr.utils;

import com.nexus.hr.model.entities.EmployeeLeaveAllocation;
import com.nexus.hr.model.entities.HrEntity;
import com.nexus.hr.model.enums.LeaveType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Utility class to handle leave allocation calculations
 */
@Component
@Slf4j
public class LeaveAllocationUtils {

    /**
     * Initialize leave allocations for a new employee based on the current month and year
     * <p>
     * Allocations:
     * - EARNED_LEAVE: 15 days/year (1.25 days/month)
     * - SICK_LEAVE: 13 days/year (prorated for current year)
     * - BEREAVEMENT_LEAVE: 3 days
     * - MATERNITY_LEAVE: 30 days
     * - PATERNITY_LEAVE: 5 days
     */
    public void initializeLeaveAllocations(HrEntity hrEntity) {
        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();

        log.info("Initializing leave allocations for employee: {} in year: {}",
                hrEntity.getEmployeeId(), currentYear);

        // 1. EARNED_LEAVE: 1.25 days for the joining month only
        // This will accrue monthly as we add scheduled tasks for each month
        EmployeeLeaveAllocation earnedLeave = new EmployeeLeaveAllocation();
        earnedLeave.setHrEntity(hrEntity);
        earnedLeave.setLeaveType(LeaveType.EARNED_LEAVE);
        earnedLeave.setAllocatedDays(1.25); // Only this month's allocation
        earnedLeave.setUsedDays(0.0);
        earnedLeave.setRemainingDays(1.25);
        earnedLeave.setYear(currentYear);
        earnedLeave.setAllocationDate(new Timestamp(System.currentTimeMillis()));
        earnedLeave.setIsActive(Boolean.TRUE);
        earnedLeave.setIsCarryForwardable(Boolean.TRUE);
        hrEntity.getLeaveAllocations().add(earnedLeave);
        log.debug("Added EARNED_LEAVE: 1.25 days for joining month");

        // 2. SICK_LEAVE: 13 days per year (full annual allocation)
        EmployeeLeaveAllocation sickLeave = new EmployeeLeaveAllocation();
        sickLeave.setHrEntity(hrEntity);
        sickLeave.setLeaveType(LeaveType.SICK_LEAVE);
        sickLeave.setAllocatedDays(13.0); // Full annual allocation
        sickLeave.setUsedDays(0.0);
        sickLeave.setRemainingDays(13.0);
        sickLeave.setYear(currentYear);
        sickLeave.setAllocationDate(new Timestamp(System.currentTimeMillis()));
        sickLeave.setIsActive(Boolean.TRUE);
        sickLeave.setIsCarryForwardable(Boolean.FALSE); // Sick leave typically doesn't carry forward
        hrEntity.getLeaveAllocations().add(sickLeave);
        log.debug("Added SICK_LEAVE: 13.0 days (full annual allocation)");

        // 3. BEREAVEMENT_LEAVE: 3 days
        EmployeeLeaveAllocation bereavementLeave = new EmployeeLeaveAllocation();
        bereavementLeave.setHrEntity(hrEntity);
        bereavementLeave.setLeaveType(LeaveType.BEREAVEMENT_LEAVE);
        bereavementLeave.setAllocatedDays(3.0);
        bereavementLeave.setUsedDays(0.0);
        bereavementLeave.setRemainingDays(3.0);
        bereavementLeave.setYear(currentYear);
        bereavementLeave.setAllocationDate(new Timestamp(System.currentTimeMillis()));
        bereavementLeave.setIsActive(Boolean.TRUE);
        bereavementLeave.setIsCarryForwardable(Boolean.FALSE);
        hrEntity.getLeaveAllocations().add(bereavementLeave);
        log.debug("Added BEREAVEMENT_LEAVE: 3 days");

        // 4. MATERNITY_LEAVE: 30 days
        EmployeeLeaveAllocation maternityLeave = new EmployeeLeaveAllocation();
        maternityLeave.setHrEntity(hrEntity);
        maternityLeave.setLeaveType(LeaveType.MATERNITY_LEAVE);
        maternityLeave.setAllocatedDays(30.0);
        maternityLeave.setUsedDays(0.0);
        maternityLeave.setRemainingDays(30.0);
        maternityLeave.setYear(currentYear);
        maternityLeave.setAllocationDate(new Timestamp(System.currentTimeMillis()));
        maternityLeave.setIsActive(Boolean.TRUE);
        maternityLeave.setIsCarryForwardable(Boolean.FALSE);
        hrEntity.getLeaveAllocations().add(maternityLeave);
        log.debug("Added MATERNITY_LEAVE: 30 days");

        // 5. PATERNITY_LEAVE: 5 days
        EmployeeLeaveAllocation paternityLeave = new EmployeeLeaveAllocation();
        paternityLeave.setHrEntity(hrEntity);
        paternityLeave.setLeaveType(LeaveType.PATERNITY_LEAVE);
        paternityLeave.setAllocatedDays(5.0);
        paternityLeave.setUsedDays(0.0);
        paternityLeave.setRemainingDays(5.0);
        paternityLeave.setYear(currentYear);
        paternityLeave.setAllocationDate(new Timestamp(System.currentTimeMillis()));
        paternityLeave.setIsActive(Boolean.TRUE);
        paternityLeave.setIsCarryForwardable(Boolean.FALSE);
        hrEntity.getLeaveAllocations().add(paternityLeave);
        log.debug("Added PATERNITY_LEAVE: 5 days");

        log.info("✓ Successfully initialized all leave allocations for employee: {}", hrEntity.getEmployeeId());
    }
}




