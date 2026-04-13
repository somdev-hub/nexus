package com.nexus.hr.views;

import com.nexus.hr.model.entities.Bonus;
import com.nexus.hr.model.entities.Deduction;
import com.nexus.hr.payload.PdfTemplateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for rendering Thymeleaf templates for HR documents (PDFs)
 * Handles joining letters, compensation cards, salary slips, promotion letters,
 * and letters of intent
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTemplateService {

    private final ITemplateEngine templateEngine;

    private static final String DATE_FORMAT = "dd-MM-yyyy";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    /**
     * Renders a joining letter template with the provided data
     *
     * @param templateData The data to populate the template
     * @return Rendered HTML content
     */
    public String renderJoiningLetter(PdfTemplateDto templateData) {
        try {
            log.info("Rendering joining letter template for employee ID: {}", templateData.getEmployeeId());

            Context context = buildJoiningLetterContext(templateData);
            String content = templateEngine.process("documents/joining-letter", context);

            log.info("Successfully rendered joining letter template");
            return content;
        } catch (Exception e) {
            log.error("Error rendering joining letter template for employee ID: {}", templateData.getEmployeeId(), e);
            throw new RuntimeException("Failed to render joining letter template: " + e.getMessage(), e);
        }
    }

    /**
     * Renders a letter of intent template with the provided data
     *
     * @param templateData The data to populate the template
     * @return Rendered HTML content
     */
    public String renderLetterOfIntent(PdfTemplateDto templateData) {
        try {
            log.info("Rendering letter of intent template for employee ID: {}", templateData.getEmployeeId());

            Context context = buildLetterOfIntentContext(templateData);
            String content = templateEngine.process("documents/letter-of-intent", context);

            log.info("Successfully rendered letter of intent template");
            return content;
        } catch (Exception e) {
            log.error("Error rendering letter of intent template for employee ID: {}", templateData.getEmployeeId(), e);
            throw new RuntimeException("Failed to render letter of intent template: " + e.getMessage(), e);
        }
    }

    /**
     * Renders a compensation card template with the provided data
     *
     * @param templateData The data to populate the template
     * @return Rendered HTML content
     */
    public String renderCompensationCard(PdfTemplateDto templateData) {
        try {
            log.info("Rendering compensation card template for employee ID: {}", templateData.getEmployeeId());

            Context context = buildCompensationCardContext(templateData);
            String content = templateEngine.process("documents/compensation-card", context);

            log.info("Successfully rendered compensation card template");
            return content;
        } catch (Exception e) {
            log.error("Error rendering compensation card template for employee ID: {}", templateData.getEmployeeId(),
                    e);
            throw new RuntimeException("Failed to render compensation card template: " + e.getMessage(), e);
        }
    }

    /**
     * Renders a promotion letter template with the provided data
     *
     * @param templateData The data to populate the template
     * @return Rendered HTML content
     */
    public String renderPromotionLetter(PdfTemplateDto templateData) {
        try {
            log.info("Rendering promotion letter template for employee ID: {}", templateData.getEmployeeId());

            Context context = buildPromotionLetterContext(templateData);
            String content = templateEngine.process("documents/promotion-letter", context);

            log.info("Successfully rendered promotion letter template");
            return content;
        } catch (Exception e) {
            log.error("Error rendering promotion letter template for employee ID: {}", templateData.getEmployeeId(), e);
            throw new RuntimeException("Failed to render promotion letter template: " + e.getMessage(), e);
        }
    }

    /**
     * Renders a salary slip template with the provided data
     *
     * @param templateData The data to populate the template
     * @return Rendered HTML content
     */
    public String renderSalarySlip(PdfTemplateDto templateData) {
        try {
            log.info("Rendering salary slip template for employee ID: {}", templateData.getEmployeeId());

            Context context = buildSalarySlipContext(templateData);
            String content = templateEngine.process("documents/salary-slip", context);

            log.info("Successfully rendered salary slip template");
            return content;
        } catch (Exception e) {
            log.error("Error rendering salary slip template for employee ID: {}", templateData.getEmployeeId(), e);
            throw new RuntimeException("Failed to render salary slip template: " + e.getMessage(), e);
        }
    }

    // ==================== CONTEXT BUILDERS ====================

    private Context buildJoiningLetterContext(PdfTemplateDto data) {
        Context context = new Context();
        context.setVariable("organizationName", defaultIfEmpty(data.getOrganizationName()));
        context.setVariable("organizationAddress", defaultIfEmpty(data.getOrganizationAddress()));
        context.setVariable("effectiveDate", formatDate(data.getEffectiveFrom()));
        context.setVariable("employeeId", data.getEmployeeId());
        context.setVariable("employeeName", defaultIfEmpty(data.getEmployeeName()));
        context.setVariable("position", defaultIfEmpty(data.getPosition()));
        context.setVariable("department", defaultIfEmpty(data.getDepartment()));
        context.setVariable("remarks", defaultIfEmpty(data.getRemarks(), "N/A"));
        context.setVariable("hrContactEmail", defaultIfEmpty(data.getHrContactEmail()));
        context.setVariable("hrContactPhone", defaultIfEmpty(data.getHrContactPhone()));
        return context;
    }

    private Context buildLetterOfIntentContext(PdfTemplateDto data) {
        Context context = new Context();
        context.setVariable("organizationName", defaultIfEmpty(data.getOrganizationName()));
        context.setVariable("organizationAddress", defaultIfEmpty(data.getOrganizationAddress()));
        context.setVariable("currentDate", LocalDate.now().format(DATE_FORMATTER));
        context.setVariable("effectiveDate", formatDate(data.getEffectiveFrom()));
        context.setVariable("employeeName", defaultIfEmpty(data.getEmployeeName()));
        context.setVariable("position", defaultIfEmpty(data.getPosition()));
        context.setVariable("department", defaultIfEmpty(data.getDepartment()));
        context.setVariable("employeeId", data.getEmployeeId());
        context.setVariable("hrContactEmail", defaultIfEmpty(data.getHrContactEmail()));
        context.setVariable("hrContactPhone", defaultIfEmpty(data.getHrContactPhone()));

        // Compensation details if available
        if (data.getBasePay() != null || data.getNetPay() != null) {
            Map<String, String> compensation = new HashMap<>();
            compensation.put("annualPackage", defaultIfEmpty(data.getAnnualPackage(),
                    formatCurrency(data.getNetPay())));
            compensation.put("basePay", formatCurrency(data.getBasePay()));
            compensation.put("hra", formatCurrency(data.getHra()));
            compensation.put("netPay", formatCurrency(data.getNetPay()));
            context.setVariable("compensation", compensation);
            context.setVariable("hasCompensation", true);
        } else {
            context.setVariable("hasCompensation", false);
        }

        return context;
    }

    private Context buildCompensationCardContext(PdfTemplateDto data) {
        Context context = new Context();
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        String effectiveDate = formatDate(data.getEffectiveFrom());

        context.setVariable("organizationName", defaultIfEmpty(data.getOrganizationName()));
        context.setVariable("organizationAddress", defaultIfEmpty(data.getOrganizationAddress()));
        context.setVariable("employeeId", data.getEmployeeId());
        context.setVariable("employeeName", defaultIfEmpty(data.getEmployeeName(), "N/A"));
        context.setVariable("department", defaultIfEmpty(data.getDepartment(), "N/A"));
        context.setVariable("position", defaultIfEmpty(data.getPosition(), "N/A"));
        context.setVariable("effectiveDate", effectiveDate);
        context.setVariable("documentDate", currentDate);
        context.setVariable("annualPackage", defaultIfEmpty(data.getAnnualPackage(),
                formatCurrency(data.getGrossPay())));
        context.setVariable("monthlyNetPay", formatCurrency(data.getNetPay()));

        // Salary breakdown
        context.setVariable("basePay", formatCurrency(data.getBasePay()));
        context.setVariable("hra", formatCurrency(data.getHra()));
        context.setVariable("pf", formatCurrency(data.getPf()));
        context.setVariable("gratuity", formatCurrency(data.getGratuity()));
        context.setVariable("grossSalary", formatCurrency(data.getGrossPay()));
        context.setVariable("netPay", formatCurrency(data.getNetPay()));

        // Bonuses
        List<Map<String, Object>> bonusList = buildBonusList(data.getBonuses());
        context.setVariable("bonuses", bonusList);
        context.setVariable("hasBonuses", !bonusList.isEmpty());

        // Deductions
        List<Map<String, Object>> deductionList = buildDeductionList(data.getDeductions());
        context.setVariable("deductions", deductionList);
        context.setVariable("hasDeductions", !deductionList.isEmpty());

        context.setVariable("hrContactEmail", defaultIfEmpty(data.getHrContactEmail()));
        context.setVariable("hrContactPhone", defaultIfEmpty(data.getHrContactPhone()));

        return context;
    }

    private Context buildPromotionLetterContext(PdfTemplateDto data) {
        Context context = new Context();
        LocalDate currentDate = LocalDate.now();
        String currentDateStr = currentDate.format(DATE_FORMATTER);
        String effectiveDateStr = formatDate(data.getEffectiveFrom());

        context.setVariable("organizationName", defaultIfEmpty(data.getOrganizationName()));
        context.setVariable("organizationAddress", defaultIfEmpty(data.getOrganizationAddress()));
        context.setVariable("hrContactEmail", defaultIfEmpty(data.getHrContactEmail()));
        context.setVariable("hrContactPhone", defaultIfEmpty(data.getHrContactPhone()));
        context.setVariable("currentDate", currentDateStr);
        context.setVariable("employeeId", data.getEmployeeId());
        context.setVariable("department", defaultIfEmpty(data.getDepartment()));
        context.setVariable("previousPosition", defaultIfEmpty(data.getPreviousTitle(), "Senior Position"));
        context.setVariable("newPosition", defaultIfEmpty(data.getTitle()));
        context.setVariable("effectiveDate", effectiveDateStr);
        context.setVariable("basePay", formatCurrency(data.getBasePay()));
        context.setVariable("hra", formatCurrency(data.getHra()));
        context.setVariable("pf", formatCurrency(data.getPf()));
        context.setVariable("netMonthlyPay", formatCurrency(data.getNetPay()));
        context.setVariable("annualPackage", formatCurrency(
                data.getAnnualPackage() != null ? Double.parseDouble(data.getAnnualPackage()) : data.getGrossPay()));

        return context;
    }

    private Context buildSalarySlipContext(PdfTemplateDto data) {
        Context context = new Context();
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        String paymentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM, yyyy"));

        context.setVariable("organizationName", defaultIfEmpty(data.getOrganizationName()));
        context.setVariable("organizationAddress", defaultIfEmpty(data.getOrganizationAddress()));
        context.setVariable("employeeId", data.getEmployeeId());
        context.setVariable("employeeName", defaultIfEmpty(data.getEmployeeName()));
        context.setVariable("department", defaultIfEmpty(data.getDepartment()));
        context.setVariable("position", defaultIfEmpty(data.getPosition()));
        context.setVariable("paymentMonth", paymentMonth);
        context.setVariable("currentDate", currentDate);
        context.setVariable("basePay", formatCurrency(data.getBasePay()));
        context.setVariable("hra", formatCurrency(data.getHra()));
        context.setVariable("pf", formatCurrency(data.getPf()));
        context.setVariable("grossSalary", formatCurrency(data.getGrossPay()));
        context.setVariable("netPay", formatCurrency(data.getNetPay()));
        context.setVariable("hrContactEmail", defaultIfEmpty(data.getHrContactEmail()));
        context.setVariable("hrContactPhone", defaultIfEmpty(data.getHrContactPhone()));

        // Earnings
        List<Map<String, Object>> earningsList = buildBonusList(data.getBonuses());
        context.setVariable("earnings", earningsList);

        // Deductions
        List<Map<String, Object>> deductionList = buildDeductionList(data.getDeductions());
        context.setVariable("deductions", deductionList);

        return context;
    }

    // ==================== HELPER METHODS ====================

    private List<Map<String, Object>> buildBonusList(List<Bonus> bonuses) {
        if (CollectionUtils.isEmpty(bonuses)) {
            return List.of();
        }

        return bonuses.stream()
                .map(bonus -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", defaultIfEmpty(bonus.getBonusType(), "Bonus"));

                    if (bonus.getAmount() != null) {
                        item.put("amount", formatCurrency(bonus.getAmount()));
                    } else if (bonus.getPercentageOfSalary() != null) {
                        item.put("amount", bonus.getPercentageOfSalary() + "%");
                    } else {
                        item.put("amount", "-");
                    }

                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildDeductionList(List<Deduction> deductions) {
        if (CollectionUtils.isEmpty(deductions)) {
            return List.of();
        }

        return deductions.stream()
                .map(deduction -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", defaultIfEmpty(deduction.getDeductionType(), "Deduction"));

                    if (deduction.getAmount() != null) {
                        item.put("amount", formatCurrency(deduction.getAmount()));
                    } else if (deduction.getPercentageOfSalary() != null) {
                        item.put("amount", deduction.getPercentageOfSalary() + "%");
                    } else {
                        item.put("amount", "-");
                    }

                    return item;
                })
                .collect(Collectors.toList());
    }

    private String formatDate(Object date) {
        if (date == null) {
            return LocalDate.now().format(DATE_FORMATTER);
        }

        if (date instanceof LocalDate) {
            return ((LocalDate) date).format(DATE_FORMATTER);
        }

        return date.toString();
    }

    private String formatCurrency(Number amount) {
        if (amount == null) {
            return "₹ 0.00";
        }
        return CURRENCY_FORMAT.format(amount);
    }

    private String defaultIfEmpty(String value) {
        return ObjectUtils.isEmpty(value) ? "" : value;
    }

    private String defaultIfEmpty(String value, String defaultValue) {
        return ObjectUtils.isEmpty(value) ? defaultValue : value;
    }
}
