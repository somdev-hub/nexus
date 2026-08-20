package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.enums.LeaveType;
import com.nexus.hr.payload.response.HeroAnalyticsResponseDto;
import com.nexus.hr.repository.*;
import com.nexus.hr.service.interfaces.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String[] MONTHS = {
            "", "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
    };
    private final TimeManagementRepo timeManagementRepo;
    private final EmployeeLeavesRepo employeeLeavesRepo;
    private final PayrollRepo payrollRepo;
    private final HrEntityRepo hrEntityRepo;
    private final HrRequestRepo hrRequestRepo;

    private static HeroAnalyticsResponseDto.Values fillHeroAnalyticsResponseValues(Integer presentValue, Integer differenceValue, String comparisonWith) {
        HeroAnalyticsResponseDto.Values presentEmployeesCountValues = new HeroAnalyticsResponseDto.Values();
        presentEmployeesCountValues.setValue(presentValue);
        presentEmployeesCountValues.setDifference(differenceValue);
        presentEmployeesCountValues.setTrend(differenceValue > 0 ? HeroAnalyticsResponseDto.Trend.INCREMENT : differenceValue < 0 ? HeroAnalyticsResponseDto.Trend.DECREMENT : HeroAnalyticsResponseDto.Trend.STABLE);
        presentEmployeesCountValues.setComparisonWith(comparisonWith);
        return presentEmployeesCountValues;
    }

    @Override
    public ResponseEntity<?> getEmployeeAvgStrength(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            // Calculate last 12 months date range
            YearMonth currentMonth = YearMonth.now();
            YearMonth startMonth = currentMonth.minusMonths(11); // Go back 11 months (12 total including current)

            Integer startYear = startMonth.getYear();
            Integer startMonthValue = startMonth.getMonthValue();
            Integer endYear = currentMonth.getYear();
            Integer endMonthValue = currentMonth.getMonthValue();

            // Get total active employees in organization
            Long totalEmployees = timeManagementRepo.countActiveEmployeesByOrg(orgId);

            // Get month-wise employee presence data for last 12 months
            List<Object[]> monthWiseData = timeManagementRepo.findMonthWiseEmployeePresenceLastYear(orgId, startYear, startMonthValue, endYear, endMonthValue);

            // Build ordered map of all 12 months
            Map<String, Long> monthWiseStrength = new LinkedHashMap<>();
            YearMonth tempMonth = startMonth;
            while (!tempMonth.isAfter(currentMonth)) {
                String monthName = MONTHS[tempMonth.getMonthValue()];
                monthWiseStrength.put(monthName, 0L);
                tempMonth = tempMonth.plusMonths(1);
            }

            // Override with actual data
            for (Object[] row : monthWiseData) {
                int month = ((Number) row[0]).intValue();
                Long count = ((Number) row[1]).longValue();
                if (month > 0 && month <= 12) {
                    monthWiseStrength.put(MONTHS[month], count);
                }
            }

            // Build response map
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("totalEmployees", totalEmployees != null ? totalEmployees : 0);
            response.put("monthWiseStrength", monthWiseStrength);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error fetching employee strength data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getLeaveTypeDistribution(Long orgId, String monthYear) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(monthYear)) {
            return ResponseEntity.badRequest().body("Organization ID and Month-Year are required");
        }
        try {
            Map<String, Integer> monthYearMap = parseMonthYear(monthYear);
            Integer month = monthYearMap.get("month");
            Integer year = monthYearMap.get("year");

            if (month == null || year == null) {
                return ResponseEntity.badRequest()
                        .body("Invalid month-year format. Received: '" + monthYear + "'. Expected format: 'APRIL 2026'");
            }

            System.out.println("DEBUG: Fetching leave distribution for orgId=" + orgId + ", month=" + month + ", year=" + year);

            // Initialize all leave types with 0 from enum
            Map<String, Long> leaveDistribution = new LinkedHashMap<>();
            for (LeaveType leaveType : LeaveType.values()) {
                leaveDistribution.put(leaveType.toString(), 0L);
            }

            // Get leave type distribution for the specified month
            List<Object[]> leaveData = employeeLeavesRepo.getLeaveDistributionByMonthYear(orgId, month, year);
            System.out.println("DEBUG: Leave data rows retrieved: " + leaveData.size());

            // Override with actual data
            for (Object[] row : leaveData) {
                Object leaveType = row[0];
                Long count = ((Number) row[1]).longValue();

                if (leaveType != null) {
                    // Use enum value as key directly
                    String leaveTypeKey = leaveType.toString();
                    leaveDistribution.put(leaveTypeKey, count);
                    System.out.println("DEBUG: Added " + leaveTypeKey + " = " + count);
                }
            }

            System.out.println("DEBUG: Final distribution: " + leaveDistribution);
            return ResponseEntity.ok(leaveDistribution);
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in getLeaveTypeDistribution: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error fetching leave distribution data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getEmployeeCheckInCheckOut(Long orgId, String monthYear) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(monthYear)) {
            return ResponseEntity.badRequest().body("Organization ID and Month-Year are required");
        }
        try {
            Map<String, Integer> monthYearMap = parseMonthYear(monthYear);
            Integer month = monthYearMap.get("month");
            Integer year = monthYearMap.get("year");

            if (month == null || year == null) {
                return ResponseEntity.badRequest()
                        .body("Invalid month-year format. Received: '" + monthYear + "'. Expected format: 'APRIL 2026'");
            }

            // Get day-wise check-in/checkout data
            List<Object[]> checkInOutData = timeManagementRepo.getCheckInCheckOutByDayForMonth(orgId, month, year);

            // Initialize all weekdays with default values
            Map<String, Object> response = new LinkedHashMap<>();
            String[] weekdays = {"monday", "tuesday", "wednesday", "thursday", "friday"};
            for (String day : weekdays) {
                Map<String, String> dayData = new LinkedHashMap<>();
                dayData.put("checkIn", "00:00");
                dayData.put("checkout", "00:00");
                response.put(day, dayData);
            }

            // Override with actual data (skip weekends)
            for (Object[] row : checkInOutData) {
                String dayName = (String) row[0];
                Double avgCheckInMinutes = row[1] != null ? ((Number) row[1]).doubleValue() : null;
                Double avgCheckOutMinutes = row[2] != null ? ((Number) row[2]).doubleValue() : null;

                if (dayName != null) {
                    String dayNameLower = dayName.toLowerCase();
                    // Skip Saturday and Sunday
                    if ("saturday".equals(dayNameLower) || "sunday".equals(dayNameLower)) {
                        continue;
                    }

                    Map<String, String> dayData = new LinkedHashMap<>();
                    dayData.put("checkIn", convertMinutesToTime(avgCheckInMinutes));
                    dayData.put("checkout", convertMinutesToTime(avgCheckOutMinutes));
                    response.put(dayNameLower, dayData);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching check-in/checkout data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getEmployeeBreakStartEnd(Long orgId, String monthYear) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(monthYear)) {
            return ResponseEntity.badRequest().body("Organization ID and Month-Year are required");
        }
        try {
            Map<String, Integer> monthYearMap = parseMonthYear(monthYear);
            Integer month = monthYearMap.get("month");
            Integer year = monthYearMap.get("year");

            if (month == null || year == null) {
                return ResponseEntity.badRequest()
                        .body("Invalid month-year format. Received: '" + monthYear + "'. Expected format: 'APRIL 2026'");
            }

            // Get day-wise break start/end data
            List<Object[]> breakData = timeManagementRepo.getBreakStartEndByDayForMonth(orgId, month, year);

            // Initialize all weekdays with default values
            Map<String, Object> response = new LinkedHashMap<>();
            String[] weekdays = {"monday", "tuesday", "wednesday", "thursday", "friday"};
            for (String day : weekdays) {
                Map<String, String> dayData = new LinkedHashMap<>();
                dayData.put("breakStart", "00:00");
                dayData.put("breakEnd", "00:00");
                response.put(day, dayData);
            }

            // Override with actual data (skip weekends)
            for (Object[] row : breakData) {
                String dayName = (String) row[0];
                Double avgBreakStartMinutes = row[1] != null ? ((Number) row[1]).doubleValue() : null;
                Double avgBreakEndMinutes = row[2] != null ? ((Number) row[2]).doubleValue() : null;

                if (dayName != null) {
                    String dayNameLower = dayName.toLowerCase();
                    // Skip Saturday and Sunday
                    if ("saturday".equals(dayNameLower) || "sunday".equals(dayNameLower)) {
                        continue;
                    }

                    Map<String, String> dayData = new LinkedHashMap<>();
                    dayData.put("breakStart", convertMinutesToTime(avgBreakStartMinutes));
                    dayData.put("breakEnd", convertMinutesToTime(avgBreakEndMinutes));
                    response.put(dayNameLower, dayData);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching break data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPayrollYearly(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            // Calculate last 12 months date range
            YearMonth currentMonth = YearMonth.now();
            YearMonth startMonth = currentMonth.minusMonths(11);

            Integer startYear = startMonth.getYear();
            Integer startMonthValue = startMonth.getMonthValue();
            Integer endYear = currentMonth.getYear();
            Integer endMonthValue = currentMonth.getMonthValue();

            // Get monthly average net payroll data
            List<Object[]> payrollData = payrollRepo.getMonthlyAverageNetPayroll(orgId, startYear, startMonthValue, endYear, endMonthValue);

            // Build ordered map of last 12 months
            Map<String, Double> monthWisePayroll = new LinkedHashMap<>();
            YearMonth tempMonth = startMonth;
            while (!tempMonth.isAfter(currentMonth)) {
                String monthName = MONTHS[tempMonth.getMonthValue()];
                monthWisePayroll.put(monthName, 0.0);
                tempMonth = tempMonth.plusMonths(1);
            }

            // Override with actual data
            for (Object[] row : payrollData) {
                Integer month = row[0] != null ? ((Number) row[0]).intValue() : null;
                Double avgNetPay = row[2] != null ? ((Number) row[2]).doubleValue() : null;

                if (month != null && month >= 1 && month <= 12) {
                    monthWisePayroll.put(MONTHS[month], avgNetPay != null ? avgNetPay : 0.0);
                }
            }

            return ResponseEntity.ok(monthWisePayroll);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error fetching payroll data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPayrollRoleWise(Long orgId, Map<String, List<Long>> roleEmpIdsMappings) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(roleEmpIdsMappings)) {
            return ResponseEntity.badRequest().body("Organization ID and role-employee mappings are required");
        }
        try {
            // Calculate last 12 months date range
            YearMonth currentMonth = YearMonth.now();
            YearMonth startMonth = currentMonth.minusMonths(11);

            Integer startYear = startMonth.getYear();
            Integer startMonthValue = startMonth.getMonthValue();
            Integer endYear = currentMonth.getYear();
            Integer endMonthValue = currentMonth.getMonthValue();

            // Build response map with quarters
            Map<String, Map<String, Double>> response = new LinkedHashMap<>();
            response.put("q1", new LinkedHashMap<>());
            response.put("q2", new LinkedHashMap<>());
            response.put("q3", new LinkedHashMap<>());
            response.put("q4", new LinkedHashMap<>());

            // Initialize all roles with 0 for each quarter
            for (String role : roleEmpIdsMappings.keySet()) {
                for (int q = 1; q <= 4; q++) {
                    response.get("q" + q).put(role, 0.0);
                }
            }

            // Query payroll data for each role
            for (Map.Entry<String, List<Long>> entry : roleEmpIdsMappings.entrySet()) {
                String role = entry.getKey();
                List<Long> empIds = entry.getValue();

                if (!empIds.isEmpty()) {
                    List<Object[]> quarterlyData = payrollRepo.getQuarterlyAverageNetPayrollByEmployees(empIds, startYear, startMonthValue, endYear, endMonthValue);

                    // Map quarter data to response
                    for (Object[] row : quarterlyData) {
                        int quarter = ((Number) row[0]).intValue();
                        Double avgNetPay = ((Number) row[1]).doubleValue();

                        if (quarter >= 1 && quarter <= 4) {
                            response.get("q" + quarter).put(role, avgNetPay);
                        }
                    }
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error fetching role-wise payroll data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getOvertimeAnomaly(Long orgId) {
        return null;
    }

    @Override
    public ResponseEntity<?> getLeavesDepartmentWise(Long orgId, String monthYear) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(monthYear)) {
            return ResponseEntity.badRequest().body("Organization ID and Month-Year are required");
        }
        try {
            Map<String, Integer> monthYearMap = parseMonthYear(monthYear);
            Integer month = monthYearMap.get("month");
            Integer year = monthYearMap.get("year");

            if (month == null || year == null) {
                return ResponseEntity.badRequest()
                        .body("Invalid month-year format. Received: '" + monthYear + "'. Expected format: 'APRIL 2026'");
            }

            YearMonth targetMonth = YearMonth.of(year, month);
            LocalDate startDate = targetMonth.atDay(1);
            LocalDate endDateExclusive = targetMonth.plusMonths(1).atDay(1);

            List<Object[]> departmentData = employeeLeavesRepo.getAverageLeavesByDepartment(orgId, startDate, endDateExclusive);

            Map<String, Double> response = new LinkedHashMap<>();
            for (Object[] row : departmentData) {
                String department = row[0] != null ? row[0].toString() : "UNKNOWN";
                Double avgLeaves = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                response.put(department, avgLeaves);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error fetching department-wise leaves data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getLeavesRoleWise(Long orgId, Map<String, List<Long>> roleEmpIdMap, String monthYear) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(roleEmpIdMap) || ObjectUtils.isEmpty(monthYear)) {
            return ResponseEntity.badRequest().body("Organization ID, role-employee mappings and Month-Year are required");
        }
        try {
            Map<String, Integer> monthYearMap = parseMonthYear(monthYear);
            Integer month = monthYearMap.get("month");
            Integer year = monthYearMap.get("year");

            if (month == null || year == null) {
                return ResponseEntity.badRequest()
                        .body("Invalid month-year format. Received: '" + monthYear + "'. Expected format: 'APRIL 2026'");
            }

            YearMonth targetMonth = YearMonth.of(year, month);
            LocalDate startDate = targetMonth.atDay(1);
            LocalDate endDateExclusive = targetMonth.plusMonths(1).atDay(1);

            Map<String, Double> response = new LinkedHashMap<>();
            for (String role : roleEmpIdMap.keySet()) {
                response.put(role, 0.0);
            }

            for (Map.Entry<String, List<Long>> entry : roleEmpIdMap.entrySet()) {
                String role = entry.getKey();
                List<Long> empIds = entry.getValue();

                if (empIds != null && !empIds.isEmpty()) {
                    Double avgLeaves = employeeLeavesRepo.getAverageLeavesByEmployeeIds(orgId, empIds, startDate, endDateExclusive);
                    response.put(role, avgLeaves != null ? avgLeaves : 0.0);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error fetching role-wise leaves data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getHeroAnalytics(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            HeroAnalyticsResponseDto response = new HeroAnalyticsResponseDto();
            Integer employeeCount = hrEntityRepo.countAllByOrgAndIsActiveTrue(orgId);
            Integer diffOfEmployee = hrEntityRepo.getHrEntityCountDiffThisWeekVsPrevious(orgId);
            HeroAnalyticsResponseDto.Values employeeCountValues = fillHeroAnalyticsResponseValues(
                    employeeCount, diffOfEmployee, "Previous Week"
            );
            response.setTotalEmployees(employeeCountValues);

            LocalDate date = LocalDate.now();
            Integer presentEmployeesCount = hrEntityRepo.countPresentEmployees(orgId, date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            Integer presentEmployeesCountDiffTodayVsYesterday = hrEntityRepo.getPresentEmployeesCountDiffTodayVsYesterday(orgId, date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            HeroAnalyticsResponseDto.Values values = fillHeroAnalyticsResponseValues(presentEmployeesCount, presentEmployeesCountDiffTodayVsYesterday, "Yesterday");
            response.setPresentEmployees(values);

            Integer onLeaveEmployeesCount= employeeCount-presentEmployeesCount;
            Integer previousDayOnLeaveEmployeesCount = hrEntityRepo.getPreviousDayOnLeaveEmployeesCount(orgId, date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            Integer onLeaveEmployeesCountDiffTodayVsYesterday = onLeaveEmployeesCount - previousDayOnLeaveEmployeesCount;
            HeroAnalyticsResponseDto.Values onLeaveEmployeesValues = fillHeroAnalyticsResponseValues(onLeaveEmployeesCount, onLeaveEmployeesCountDiffTodayVsYesterday, "Yesterday");
            response.setOnLeaveEmployees(onLeaveEmployeesValues);

            Integer openHrRequests = hrRequestRepo.countOpenRequestsByOrgId(orgId);
            Integer hrRequestsCountDiffWeekly = hrRequestRepo.countDiffInPrevWeekAndThisWeekHrRequests(orgId);
            HeroAnalyticsResponseDto.Values hrRequestValues = fillHeroAnalyticsResponseValues(openHrRequests, hrRequestsCountDiffWeekly, "Previous Week");
            response.setOpenHrRequests(hrRequestValues);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsServiceImpl",
                    "Error fetching hero analytics data",
                    "getHeroAnalytics",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getWeeklyEmployeeStrength(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            List<Object[]> weeklyData = timeManagementRepo.getWeeklyEmployeeStrength(orgId);

            // Initialize all 7 days with default values
            Map<String, Object> response = new LinkedHashMap<>();
            String[] weekdays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String day : weekdays) {
                response.put(day, 0L);
            }

            // Override with actual data
            for (Object[] row : weeklyData) {
                String dayName = row[0] != null ? row[0].toString() : null;
                Long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;

                if (dayName != null) {
                    response.put(dayName, count);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error fetching weekly employee strength data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getWeeklyWorkingHours(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            List<Object[]> weeklyData = timeManagementRepo.getWeeklyWorkingHours(orgId);

            // Initialize all 7 days with default values
            Map<String, Object> response = new LinkedHashMap<>();
            String[] weekdays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String day : weekdays) {
                response.put(day, 0.0);
            }

            // Override with actual data
            for (Object[] row : weeklyData) {
                String dayName = row[0] != null ? row[0].toString() : null;
                Double totalHours = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

                if (dayName != null) {
                    response.put(dayName, totalHours);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error fetching weekly working hours data: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getWeeklyCheckInCheckOut(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            List<Object[]> weeklyData = timeManagementRepo.getWeeklyCheckInCheckOut(orgId);

            // Initialize all 7 days with default values
            Map<String, Object> response = new LinkedHashMap<>();
            String[] weekdays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String day : weekdays) {
                Map<String, String> dayData = new LinkedHashMap<>();
                dayData.put("checkIn", "00:00");
                dayData.put("checkout", "00:00");
                response.put(day, dayData);
            }

            // Override with actual data
            for (Object[] row : weeklyData) {
                String dayName = row[0] != null ? row[0].toString() : null;
                Double avgCheckInMinutes = row[2] != null ? ((Number) row[2]).doubleValue() : null;
                Double avgCheckOutMinutes = row[3] != null ? ((Number) row[3]).doubleValue() : null;

                if (dayName != null) {
                    Map<String, String> dayData = new LinkedHashMap<>();
                    dayData.put("checkIn", convertMinutesToTime(avgCheckInMinutes));
                    dayData.put("checkout", convertMinutesToTime(avgCheckOutMinutes));
                    response.put(dayName, dayData);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error fetching weekly check-in/check-out data: " + e.getMessage());
        }
    }

    /**
     * Parse monthYear string in format "APRIL 2026" and return map with month and year
     * Handles both regular and URL-encoded input (e.g., "APRIL%202026")
     */
    private Map<String, Integer> parseMonthYear(String monthYear) {
        Map<String, Integer> result = new HashMap<>();
        if (monthYear == null || monthYear.trim().isEmpty()) {
            return result;
        }

        try {
            // Decode URL-encoded input (e.g., "APRIL%202026" -> "APRIL 2026")
            String decodedInput = URLDecoder.decode(monthYear, StandardCharsets.UTF_8);

            // Trim and normalize the input
            String normalizedInput = decodedInput.trim();

            // Split by whitespace (handles multiple spaces, tabs, etc.)
            String[] parts = normalizedInput.split("\\s+");
            if (parts.length != 2) {
                return result;
            }

            String monthName = parts[0].trim().toUpperCase();
            String yearStr = parts[1].trim();

            // Validate and parse year
            Integer year;
            try {
                year = Integer.parseInt(yearStr);
                if (year < 1900 || year > 2100) {
                    return result;
                }
            } catch (NumberFormatException e) {
                return result;
            }

            // Find month number from name
            for (int i = 1; i < 13; i++) {
                String monthFromArray = MONTHS[i].toUpperCase().trim();
                if (monthFromArray.equals(monthName)) {
                    result.put("month", i);
                    result.put("year", year);
                    return result;
                }
            }
        } catch (Exception e) {
            // Return empty map on parsing error
            e.printStackTrace();
        }
        return result;
    }


    /**
     * Convert minutes to HH:MM format
     */
    private String convertMinutesToTime(Double totalMinutes) {
        if (totalMinutes == null || totalMinutes.isNaN()) {
            return "00:00";
        }
        int hours = (int) (totalMinutes / 60);
        int minutes = (int) (totalMinutes % 60);
        return String.format("%02d:%02d", hours, minutes);
    }
}
