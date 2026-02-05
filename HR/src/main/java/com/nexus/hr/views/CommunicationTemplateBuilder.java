package com.nexus.hr.views;

import com.nexus.hr.model.entities.Bonus;
import com.nexus.hr.model.entities.Deduction;
import com.nexus.hr.payload.PdfTemplateDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Consolidated template builder for generating HTML templates for PDF
 * conversion and email communications
 */
@Component
public class CommunicationTemplateBuilder {

    private static final String DATE_FORMAT = "dd-MM-yyyy";
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    // ==================== EMAIL TEMPLATES ====================

    /**
     * Email template for HR initialization - Welcome email to new employee
     */
    public String buildHrInitEmailTemplate() {
        return """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2 style="color: #0066cc; border-bottom: 2px solid #0066cc; padding-bottom: 10px;">
                                Welcome to {organizationName}!
                            </h2>

                            <p>Dear <strong>{name}</strong>,</p>

                            <p>We are excited to have you join our team at {organizationName}!</p>

                            <p>Your employee details are as follows:</p>
                            <ul style="background-color: #f5f5f5; padding: 15px; border-left: 4px solid #0066cc;">
                                <li><strong>Employee ID:</strong> {employeeId}</li>
                                <li><strong>Department:</strong> {department}</li>
                                <li><strong>Position:</strong> {position}</li>
                                <li><strong>Date of Joining:</strong> {dateOfJoining}</li>
                            </ul>

                            <p>Please find attached the following documents to help you get started:</p>
                            <ol>
                                <li>Joining Letter</li>
                                <li>Letter of Intent</li>
                                <li>Compensation Card</li>
                            </ol>

                            <p>Please review these documents carefully and keep them for your records.</p>
                           \s
                            <p>If you have any questions or need assistance, feel free to reach out to us at
                            <a href="mailto:hr@nexuscorporation.com">hr@nexuscorporation.com</a>.</p>

                            <p style="margin-top: 30px;">Best regards,<br/>
                            <strong>{organizationName} HR Team</strong></p>

                            <hr style="border: none; border-top: 1px solid #ddd; margin-top: 30px;">
                            <p style="font-size: 12px; color: #666;">
                                This is an automated email. Please do not reply directly to this message.
                            </p>
                        </div>
                    </body>
                    </html>
                """;
    }

    /**
     * Email template for promotion notification
     */
    public String buildPromotionEmailTemplate() {
        return """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2 style="color: #27ae60; border-bottom: 3px solid #27ae60; padding-bottom: 10px;">
                                🎉 Congratulations on Your Promotion!
                            </h2>

                            <p>Dear <strong>{employeeName}</strong>,</p>

                            <p>We are delighted to inform you of your <strong>promotion</strong>, which is a testament to your
                            outstanding performance, dedication, and valuable contributions to {organizationName}.</p>

                            <div style="background-color: #e8f5e9; border-left: 4px solid #27ae60; padding: 15px; margin: 20px 0;">
                                <p><strong>Promotion Details:</strong></p>
                                <ul style="margin: 10px 0;">
                                    <li><strong>Previous Position:</strong> {previousPosition}</li>
                                    <li><strong>New Position:</strong> {newPosition}</li>
                                    <li><strong>Department:</strong> {department}</li>
                                    <li><strong>Effective Date:</strong> {effectiveDate}</li>
                                </ul>
                            </div>

                            <p>Your revised compensation package details are outlined below:</p>
                            <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
                                <ul style="margin: 10px 0; list-style: none; padding: 0;">
                                    <li><strong>Base Salary:</strong> {basePay}</li>
                                    <li><strong>HRA:</strong> {hra}</li>
                                    <li><strong>Net Monthly Pay:</strong> {netMonthlyPay}</li>
                                    <li><strong>Annual Package:</strong> {annualPackage}</li>
                                </ul>
                            </div>

                            <p>Please find attached the following documents:</p>
                            <ol>
                                <li>Promotion Letter</li>
                                <li>Revised Compensation Card</li>
                            </ol>

                            <p>This promotion recognizes your exceptional abilities, leadership qualities, and commitment to excellence.
                            We are confident that you will continue to demonstrate the same level of professional excellence in your new role.</p>

                            <p>Should you have any questions regarding this promotion or your revised compensation package,
                            please do not hesitate to contact our HR Department at <a href="mailto:{hrEmail}">{hrEmail}</a>.</p>

                            <p style="margin-top: 30px;">Once again, congratulations on this well-deserved promotion!<br/>
                            <strong>{organizationName} HR Team</strong></p>

                            <hr style="border: none; border-top: 1px solid #ddd; margin-top: 30px;">
                            <p style="font-size: 12px; color: #666;">
                                This is an automated email. Please do not reply directly to this message.
                            </p>
                        </div>
                    </body>
                    </html>
                """;
    }

    /**
     * Email template for reward appraisal notification
     */
    public String buildRewardAppraisalEmailTemplate() {
        return """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2 style="color: #ff9800; border-bottom: 3px solid #ff9800; padding-bottom: 10px;">
                                🏆 Reward Appraisal - Compensation Revision
                            </h2>

                            <p>Dear <strong>{employeeName}</strong>,</p>

                            <p>We are pleased to inform you that your performance has been recognized through a <strong>compensation revision</strong>
                            as part of our reward appraisal process. This reflects your outstanding contributions and dedication to {organizationName}.</p>

                            <div style="background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0;">
                                <p><strong>Appraisal Details:</strong></p>
                                <ul style="margin: 10px 0;">
                                    <li><strong>Position:</strong> {position}</li>
                                    <li><strong>Department:</strong> {department}</li>
                                    <li><strong>Appraisal Date:</strong> {appraisalDate}</li>
                                </ul>
                            </div>

                            <p>Your revised compensation package effective from <strong>{effectiveDate}</strong> is as follows:</p>
                            <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
                                <ul style="margin: 10px 0; list-style: none; padding: 0;">
                                    <li><strong>Base Salary:</strong> {basePay}</li>
                                    <li><strong>HRA:</strong> {hra}</li>
                                    <li><strong>Net Monthly Pay:</strong> {netMonthlyPay}</li>
                                    <li><strong>Annual Package:</strong> {annualPackage}</li>
                                </ul>
                            </div>

                            <p>Please find attached the revised compensation card with complete details on your benefits, bonuses,
                            deductions, and other compensation components.</p>

                            <p>Your performance continues to be an asset to our organization. We appreciate your hard work and
                            commitment, and we look forward to your continued success and growth with us.</p>

                            <p>If you have any questions regarding your revised compensation package,
                            please feel free to reach out to our HR Department at <a href="mailto:{hrEmail}">{hrEmail}</a>.</p>

                            <p style="margin-top: 30px;">Thank you for your exceptional contributions!<br/>
                            <strong>{organizationName} HR Team</strong></p>

                            <hr style="border: none; border-top: 1px solid #ddd; margin-top: 30px;">
                            <p style="font-size: 12px; color: #666;">
                                This is an automated email. Please do not reply directly to this message.
                            </p>
                        </div>
                    </body>
                    </html>
                """;
    }

    // ==================== PDF TEMPLATES ====================

    /**
     * Generate a joining letter HTML template
     */
    public String buildJoiningLetterTemplate(PdfTemplateDto templateData) {
        String effectiveDateStr = formatDate(templateData.getEffectiveFrom());

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: 'Calibri', 'Arial', sans-serif;
                            line-height: 1.6;
                            margin: 40px;
                            color: #333;
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                            border-bottom: 2px solid #0066cc;
                            padding-bottom: 20px;
                        }
                        .organization-name {
                            font-size: 24px;
                            font-weight: bold;
                            color: #0066cc;
                            margin-bottom: 5px;
                        }
                        .organization-address {
                            font-size: 12px;
                            color: #666;
                            margin-bottom: 10px;
                        }
                        .letter-content {
                            margin-top: 30px;
                        }
                        .date-section {
                            text-align: right;
                            margin-bottom: 30px;
                            font-size: 12px;
                        }
                        .recipient-address {
                            margin-bottom: 30px;
                            font-size: 12px;
                        }
                        .salutation {
                            margin-bottom: 20px;
                        }
                        .body-text {
                            text-align: justify;
                            margin-bottom: 15px;
                            font-size: 12px;
                        }
                        .details-table {
                            width: 100%%;
                            margin: 20px 0;
                            border-collapse: collapse;
                        }
                        .details-table td {
                            padding: 8px;
                            border: 1px solid #ddd;
                            font-size: 12px;
                        }
                        .details-table .label {
                            font-weight: bold;
                            background-color: #f5f5f5;
                            width: 30%%;
                        }
                        .closing {
                            margin-top: 30px;
                            font-size: 12px;
                        }
                        .signature-section {
                            margin-top: 40px;
                            font-size: 12px;
                        }
                        .signature-line {
                            margin-top: 50px;
                            border-top: 1px solid #000;
                            width: 200px;
                        }
                        .contact-info {
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #ddd;
                            font-size: 11px;
                            color: #666;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="organization-name">%s</div>
                        <div class="organization-address">%s</div>
                    </div>

                    <div class="letter-content">
                        <div class="date-section">
                            Date: %s
                        </div>

                        <div class="recipient-address">
                            Employee ID: %d<br>
                            Name: %s
                        </div>

                        <div class="salutation">
                            Dear %s,
                        </div>

                        <p class="body-text">
                            <strong>Subject: Joining Letter - Employment Offer</strong>
                        </p>

                        <p class="body-text">
                            We are pleased to inform you that your application has been accepted, and we would like to offer you
                            a position with our organization. We are confident that your skills and experience will make a valuable
                            contribution to our team.
                        </p>

                        <p class="body-text">
                            Please find below the details of your employment:
                        </p>

                        <table class="details-table">
                            <tr>
                                <td class="label">Position:</td>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <td class="label">Department:</td>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <td class="label">Date of Joining:</td>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <td class="label">Remarks:</td>
                                <td>%s</td>
                            </tr>
                        </table>

                        <p class="body-text">
                            We look forward to seeing you join our organization and contributing to our continued success.
                            If you have any questions or require any further information, please do not hesitate to contact us.
                        </p>

                        <p class="body-text">
                            Please acknowledge receipt of this letter and confirm your acceptance of the employment offer.
                        </p>

                        <div class="closing">
                            Yours sincerely,
                        </div>

                        <div class="signature-section">
                            <div class="signature-line"></div>
                            <div style="margin-top: 5px">
                                <strong>Human Resources Department</strong><br>
                                %s
                            </div>
                        </div>

                        <div class="contact-info">
                            <strong>Contact Information:</strong><br>
                            Email: %s<br>
                            Phone: %s
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        templateData.getOrganizationName(),
                        templateData.getOrganizationAddress(),
                        effectiveDateStr,
                        templateData.getEmployeeId(),
                        templateData.getEmployeeName(),
                        templateData.getEmployeeName(),
                        templateData.getPosition(),
                        templateData.getDepartment(),
                        effectiveDateStr,
                        templateData.getRemarks() != null ? templateData.getRemarks() : "N/A",
                        templateData.getOrganizationName(),
                        templateData.getHrContactEmail(),
                        templateData.getHrContactPhone());
    }

    /**
     * Generate a letter of intent HTML template
     */
    public String buildLetterOfIntentTemplate(PdfTemplateDto templateData) {
        String effectiveDateStr = formatDate(templateData.getEffectiveFrom());
        String currentDate = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern(DATE_FORMAT));

        // Build compensation details section
        StringBuilder compensationSection = new StringBuilder();
        if (templateData.getBasePay() != null || templateData.getNetPay() != null) {
            compensationSection.append("""
                    <div class="compensation-highlight">
                        <strong>Compensation Package:</strong>
                        <ul>
                    """);

            if (templateData.getAnnualPackage() != null) {
                compensationSection.append("<li><strong>Annual Package:</strong> ")
                        .append(templateData.getAnnualPackage()).append("</li>");
            }
            if (templateData.getNetPay() != null) {
                compensationSection.append("<li><strong>Gross Pay:</strong> ")
                        .append(formatCurrency(templateData.getNetPay())).append("</li>");
            }
            if (templateData.getBasePay() != null) {
                compensationSection.append("<li><strong>Base Pay:</strong> ")
                        .append(formatCurrency(templateData.getBasePay())).append("</li>");
            }
            if (templateData.getHra() != null) {
                compensationSection.append("<li><strong>HRA:</strong> ").append(formatCurrency(templateData.getHra()))
                        .append("</li>");
            }

            compensationSection.append("""
                        </ul>
                        <p style="font-size: 11px; color: #666; margin-top: 10px;">
                            * Detailed compensation breakdown will be provided in a separate compensation card document.
                        </p>
                    </div>
                    """);
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: 'Calibri', 'Arial', sans-serif;
                            line-height: 1.6;
                            margin: 40px;
                            color: #333;
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                            border-bottom: 2px solid #0066cc;
                            padding-bottom: 20px;
                        }
                        .organization-name {
                            font-size: 24px;
                            font-weight: bold;
                            color: #0066cc;
                            margin-bottom: 5px;
                        }
                        .organization-address {
                            font-size: 12px;
                            color: #666;
                            margin-bottom: 10px;
                        }
                        .letter-content {
                            margin-top: 30px;
                        }
                        .date-section {
                            text-align: right;
                            margin-bottom: 30px;
                            font-size: 12px;
                        }
                        .salutation {
                            margin-bottom: 20px;
                        }
                        .body-text {
                            text-align: justify;
                            margin-bottom: 15px;
                            font-size: 12px;
                        }
                        .terms-section {
                            margin: 20px 0;
                            padding: 15px;
                            background-color: #f9f9f9;
                            border-left: 4px solid #0066cc;
                            font-size: 12px;
                        }
                        .terms-section ul {
                            margin: 10px 0;
                            padding-left: 20px;
                        }
                        .terms-section li {
                            margin-bottom: 8px;
                        }
                        .compensation-highlight {
                            margin: 20px 0;
                            padding: 15px;
                            background-color: #e8f4f8;
                            border: 2px solid #0066cc;
                            border-radius: 5px;
                            font-size: 12px;
                        }
                        .compensation-highlight ul {
                            margin: 10px 0;
                            padding-left: 20px;
                        }
                        .compensation-highlight li {
                            margin-bottom: 6px;
                        }
                        .closing {
                            margin-top: 30px;
                            font-size: 12px;
                        }
                        .signature-section {
                            margin-top: 40px;
                            font-size: 12px;
                        }
                        .signature-lines {
                            margin-top: 50px;
                            display: flex;
                            justify-content: space-between;
                        }
                        .signature-block {
                            width: 45%%;
                        }
                        .signature-line {
                            border-top: 1px solid #000;
                            margin-bottom: 5px;
                        }
                        .contact-info {
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #ddd;
                            font-size: 11px;
                            color: #666;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="organization-name">%s</div>
                        <div class="organization-address">%s</div>
                    </div>

                    <div class="letter-content">
                        <div class="date-section">
                            Date: %s
                        </div>

                        <div class="salutation">
                            Dear %s,
                        </div>

                        <p class="body-text">
                            <strong>Subject: Letter of Intent - Employment Agreement</strong>
                        </p>

                        <p class="body-text">
                            This letter confirms our mutual intention to enter into an employment relationship effective from
                            <strong>%s</strong>. This Letter of Intent outlines the terms and conditions of your employment with
                            %s.
                        </p>

                        <div class="terms-section">
                            <strong>Terms and Conditions:</strong>
                            <ul>
                                <li><strong>Position:</strong> %s</li>
                                <li><strong>Department:</strong> %s</li>
                                <li><strong>Date of Commencement:</strong> %s</li>
                                <li><strong>Employment Type:</strong> Full-time</li>
                                <li><strong>Employee ID:</strong> %d</li>
                            </ul>
                        </div>

                        %s

                        <p class="body-text">
                            You will be entitled to all statutory and contractual benefits as per company policy. Your employment
                            is contingent upon successful completion of background verification and medical clearance, if applicable.
                        </p>

                        <p class="body-text">
                            You are expected to adhere to the company's policies and procedures as outlined in the Employee Handbook.
                            This includes maintaining confidentiality of company information, respecting intellectual property rights,
                            and following all safety and security protocols.
                        </p>

                        <p class="body-text">
                            If there are any questions or clarifications needed, please contact the Human Resources Department
                            immediately.
                        </p>

                        <div class="closing">
                            We welcome you to the organization and look forward to a productive working relationship.
                        </div>

                        <div class="signature-section">
                            <div class="signature-lines">
                                <div class="signature-block">
                                    <div class="signature-line"></div>
                                    <div>
                                        <strong>Employee Signature</strong><br>
                                        Date: ________________<br>
                                        Employee Name: %s
                                    </div>
                                </div>
                                <div class="signature-block">
                                    <div class="signature-line"></div>
                                    <div>
                                        <strong>Authorized Signatory</strong><br>
                                        Date: ________________<br>
                                        %s
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="contact-info">
                            <strong>Contact Information:</strong><br>
                            Email: %s<br>
                            Phone: %s<br><br>
                            <strong>Address:</strong> %s
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        templateData.getOrganizationName(),
                        templateData.getOrganizationAddress(),
                        currentDate,
                        templateData.getEmployeeName(),
                        effectiveDateStr,
                        templateData.getOrganizationName(),
                        templateData.getPosition(),
                        templateData.getDepartment(),
                        effectiveDateStr,
                        templateData.getEmployeeId(),
                        compensationSection.toString(),
                        templateData.getEmployeeName(),
                        templateData.getOrganizationName(),
                        templateData.getHrContactEmail(),
                        templateData.getHrContactPhone(),
                        templateData.getOrganizationAddress());
    }

    /**
     * Generate a compensation card HTML template
     */
    public String buildCompensationCardTemplate(PdfTemplateDto templateData) {
        String currentDate = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern(DATE_FORMAT));
        String effectiveDateStr = formatDate(templateData.getEffectiveFrom());

        // Build bonuses section
        StringBuilder bonusesHtml = new StringBuilder();
        if (templateData.getBonuses() != null && !templateData.getBonuses().isEmpty()) {
            for (Bonus bonus : templateData.getBonuses()) {
                bonusesHtml.append("<tr>");
                bonusesHtml.append("<td>").append(bonus.getBonusType() != null ? bonus.getBonusType() : "Bonus")
                        .append("</td>");
                if (bonus.getAmount() != null) {
                    bonusesHtml.append("<td class='amount'>").append(formatCurrency(bonus.getAmount())).append("</td>");
                } else if (bonus.getPercentageOfSalary() != null) {
                    bonusesHtml.append("<td class='amount'>").append(bonus.getPercentageOfSalary()).append("%</td>");
                } else {
                    bonusesHtml.append("<td class='amount'>-</td>");
                }
                bonusesHtml.append("</tr>");
            }
        } else {
            bonusesHtml.append(
                    "<tr><td colspan='2' style='text-align: center; color: #999'>No bonuses applicable</td></tr>");
        }

        // Build deductions section
        StringBuilder deductionsHtml = new StringBuilder();
        if (templateData.getDeductions() != null && !templateData.getDeductions().isEmpty()) {
            for (Deduction deduction : templateData.getDeductions()) {
                deductionsHtml.append("<tr>");
                deductionsHtml.append("<td>")
                        .append(deduction.getDeductionType() != null ? deduction.getDeductionType() : "Deduction")
                        .append("</td>");
                if (deduction.getAmount() != null) {
                    deductionsHtml.append("<td class='amount'>").append(formatCurrency(deduction.getAmount()))
                            .append("</td>");
                } else if (deduction.getPercentageOfSalary() != null) {
                    deductionsHtml.append("<td class='amount'>").append(deduction.getPercentageOfSalary())
                            .append("%</td>");
                } else {
                    deductionsHtml.append("<td class='amount'>-</td>");
                }
                deductionsHtml.append("</tr>");
            }
        } else {
            deductionsHtml.append(
                    "<tr><td colspan='2' style='text-align: center; color: #999'>No deductions applicable</td></tr>");
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        @page {
                            size: A4;
                            margin: 20mm;
                        }
                        body {
                            font-family: 'Segoe UI', 'Calibri', 'Arial', sans-serif;
                            line-height: 1.5;
                            margin: 0;
                            padding: 20px;
                            color: #333;
                            background: linear-gradient(135deg, #f5f7fa 0%%, #c3cfe2 100%%);
                        }
                        .compensation-card {
                            background: white;
                            border-radius: 10px;
                            box-shadow: 0 10px 30px rgba(0,0,0,0.1);
                            padding: 30px;
                            max-width: 800px;
                            margin: 0 auto;
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                            padding-bottom: 20px;
                            border-bottom: 3px solid #0066cc;
                        }
                        .organization-name {
                            font-size: 26px;
                            font-weight: bold;
                            color: #0066cc;
                            margin-bottom: 5px;
                            text-transform: uppercase;
                            letter-spacing: 1px;
                        }
                        .document-title {
                            font-size: 20px;
                            font-weight: 600;
                            color: #333;
                            margin-top: 15px;
                            text-transform: uppercase;
                            letter-spacing: 0.5px;
                        }
                        .meta-info {
                            display: flex;
                            justify-content: space-between;
                            margin-bottom: 25px;
                            padding: 15px;
                            background-color: #f8f9fa;
                            border-radius: 5px;
                            font-size: 13px;
                        }
                        .meta-info div {
                            flex: 1;
                        }
                        .meta-label {
                            font-weight: bold;
                            color: #666;
                            margin-bottom: 3px;
                        }
                        .meta-value {
                            color: #333;
                            font-weight: 600;
                        }
                        .summary-section {
                            margin-bottom: 25px;
                            padding: 20px;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            border-radius: 8px;
                            text-align: center;
                        }
                        .summary-section h3 {
                            margin: 0 0 15px 0;
                            font-size: 16px;
                            font-weight: 500;
                            opacity: 0.9;
                        }
                        .annual-package {
                            font-size: 36px;
                            font-weight: bold;
                            margin: 10px 0;
                            text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
                        }
                        .monthly-pay {
                            font-size: 18px;
                            margin-top: 10px;
                            opacity: 0.95;
                        }
                        .details-section {
                            margin-bottom: 25px;
                        }
                        .section-title {
                            font-size: 16px;
                            font-weight: bold;
                            color: #0066cc;
                            margin-bottom: 12px;
                            padding-bottom: 8px;
                            border-bottom: 2px solid #e0e0e0;
                            text-transform: uppercase;
                            letter-spacing: 0.5px;
                        }
                        .comp-table {
                            width: 100%%;
                            border-collapse: collapse;
                            margin-bottom: 20px;
                            font-size: 13px;
                        }
                        .comp-table th {
                            background-color: #f5f5f5;
                            padding: 12px;
                            text-align: left;
                            font-weight: 600;
                            color: #555;
                            border-bottom: 2px solid #ddd;
                        }
                        .comp-table td {
                            padding: 10px 12px;
                            border-bottom: 1px solid #eee;
                        }
                        .comp-table tr:hover {
                            background-color: #f9f9f9;
                        }
                        .comp-table .amount {
                            text-align: right;
                            font-weight: 600;
                            color: #333;
                        }
                        .comp-table .total-row {
                            background-color: #f0f8ff;
                            font-weight: bold;
                            border-top: 2px solid #0066cc;
                        }
                        .comp-table .total-row td {
                            padding: 14px 12px;
                            color: #0066cc;
                            font-size: 14px;
                        }
                        .breakdown-grid {
                            display: grid;
                            grid-template-columns: 1fr 1fr;
                            gap: 20px;
                            margin-bottom: 25px;
                        }
                        .breakdown-card {
                            padding: 15px;
                            background-color: #f8f9fa;
                            border-radius: 5px;
                            border-left: 4px solid #0066cc;
                        }
                        .breakdown-card.positive {
                            border-left-color: #28a745;
                        }
                        .breakdown-card.negative {
                            border-left-color: #dc3545;
                        }
                        .note {
                            margin-top: 30px;
                            padding: 15px;
                            background-color: #fff3cd;
                            border-left: 4px solid #ffc107;
                            border-radius: 4px;
                            font-size: 12px;
                            color: #856404;
                        }
                        .footer {
                            margin-top: 40px;
                            padding-top: 20px;
                            border-top: 2px solid #e0e0e0;
                            text-align: center;
                            font-size: 11px;
                            color: #666;
                        }
                        .signature-section {
                            margin-top: 40px;
                            display: flex;
                            justify-content: space-between;
                        }
                        .signature-block {
                            width: 45%%;
                            text-align: center;
                        }
                        .signature-line {
                            border-top: 1px solid #333;
                            margin-top: 50px;
                            margin-bottom: 5px;
                        }
                    </style>
                </head>
                <body>
                    <div class="compensation-card">
                        <div class="header">
                            <div class="organization-name">%s</div>
                            <div class="document-title">Compensation Card</div>
                        </div>

                        <div class="meta-info">
                            <div>
                                <div class="meta-label">Employee ID:</div>
                                <div class="meta-value">%d</div>
                            </div>
                            <div>
                                <div class="meta-label">Employee Name:</div>
                                <div class="meta-value">%s</div>
                            </div>
                            <div>
                                <div class="meta-label">Department:</div>
                                <div class="meta-value">%s</div>
                            </div>
                            <div>
                                <div class="meta-label">Position:</div>
                                <div class="meta-value">%s</div>
                            </div>
                        </div>

                        <div class="meta-info">
                            <div>
                                <div class="meta-label">Effective From:</div>
                                <div class="meta-value">%s</div>
                            </div>
                            <div>
                                <div class="meta-label">Document Date:</div>
                                <div class="meta-value">%s</div>
                            </div>
                        </div>

                        <div class="summary-section">
                            <h3>Total Annual Compensation Package</h3>
                            <div class="annual-package">%s</div>
                            <div class="monthly-pay">Net Monthly Pay: %s</div>
                        </div>

                        <div class="details-section">
                            <div class="section-title">Salary Breakdown</div>
                            <table class="comp-table">
                                <thead>
                                    <tr>
                                        <th>Component</th>
                                        <th style="text-align: right">Amount</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td>Base Pay</td>
                                        <td class="amount">%s</td>
                                    </tr>
                                    <tr>
                                        <td>House Rent Allowance (HRA)</td>
                                        <td class="amount">%s</td>
                                    </tr>
                                    <tr>
                                        <td>Provident Fund (PF)</td>
                                        <td class="amount">%s</td>
                                    </tr>
                                    <tr>
                                        <td>Gratuity</td>
                                        <td class="amount">%s</td>
                                    </tr>
                                    <tr class="total-row">
                                        <td>Gross Salary</td>
                                        <td class="amount">%s</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <div class="breakdown-grid">
                            <div class="breakdown-card positive">
                                <div class="section-title" style="border: none; padding-bottom: 5px">Bonuses & Allowances</div>
                                <table class="comp-table" style="margin-bottom: 0">
                                    <tbody>
                                        %s
                                    </tbody>
                                </table>
                            </div>

                            <div class="breakdown-card negative">
                                <div class="section-title" style="border: none; padding-bottom: 5px">Deductions</div>
                                <table class="comp-table" style="margin-bottom: 0">
                                    <tbody>
                                        %s
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <div class="details-section">
                            <table class="comp-table">
                                <tbody>
                                    <tr class="total-row">
                                        <td>NET PAY (Take Home)</td>
                                        <td class="amount">%s</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <div class="note">
                            <strong>Note:</strong> This compensation card is for reference purposes only.
                            All amounts are subject to applicable taxes and statutory deductions as per government regulations.
                            The actual take-home pay may vary based on tax declarations and deductions opted by the employee.
                        </div>

                        <div class="signature-section">
                            <div class="signature-block">
                                <div class="signature-line"></div>
                                <div style="margin-top: 5px; font-size: 12px">
                                    <strong>Employee Signature</strong><br>
                                    Date: _______________
                                </div>
                            </div>
                            <div class="signature-block">
                                <div class="signature-line"></div>
                                <div style="margin-top: 5px; font-size: 12px">
                                    <strong>HR Department</strong><br>
                                    Date: _______________
                                </div>
                            </div>
                        </div>

                        <div class="footer">
                            <p><strong>%s</strong></p>
                            <p>%s</p>
                            <p>Email: %s | Phone: %s</p>
                            <p style="margin-top: 10px; font-style: italic">
                                This is a computer-generated document and does not require a physical signature.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        templateData.getOrganizationName(),
                        templateData.getEmployeeId(),
                        templateData.getEmployeeName() != null ? templateData.getEmployeeName() : "N/A",
                        templateData.getDepartment() != null ? templateData.getDepartment() : "N/A",
                        templateData.getPosition() != null ? templateData.getPosition() : "N/A",
                        effectiveDateStr,
                        currentDate,
                        templateData.getAnnualPackage() != null ? templateData.getAnnualPackage()
                                : formatCurrency(templateData.getGrossPay()),
                        formatCurrency(templateData.getNetPay()),
                        formatCurrency(templateData.getBasePay()),
                        formatCurrency(templateData.getHra()),
                        formatCurrency(templateData.getPf()),
                        formatCurrency(templateData.getGratuity()),
                        formatCurrency(templateData.getGrossPay()),
                        bonusesHtml.toString(),
                        deductionsHtml.toString(),
                        formatCurrency(templateData.getNetPay()),
                        templateData.getOrganizationName(),
                        templateData.getOrganizationAddress(),
                        templateData.getHrContactEmail(),
                        templateData.getHrContactPhone());
    }

    /**
     * Generate a promotion letter HTML template
     */
    public String buildPromotionLetterTemplate(PdfTemplateDto templateData) {
        String effectiveDateStr = formatDate(templateData.getEffectiveFrom());
        LocalDate currentDate = LocalDate.now();
        String currentDateStr = currentDate.format(java.time.format.DateTimeFormatter.ofPattern(DATE_FORMAT));

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: 'Calibri', 'Arial', sans-serif;
                            line-height: 1.8;
                            margin: 40px;
                            color: #333;
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                            border-bottom: 3px solid #2c3e50;
                            padding-bottom: 20px;
                        }
                        .organization-name {
                            font-size: 26px;
                            font-weight: bold;
                            color: #2c3e50;
                            margin-bottom: 5px;
                        }
                        .organization-details {
                            font-size: 12px;
                            color: #666;
                            margin-top: 10px;
                        }
                        .content {
                            margin-top: 30px;
                            line-height: 1.8;
                        }
                        .date-section {
                            text-align: right;
                            margin-bottom: 30px;
                            font-size: 13px;
                        }
                        .employee-details {
                            margin-bottom: 30px;
                            font-size: 13px;
                        }
                        .greeting {
                            margin-bottom: 25px;
                            font-size: 13px;
                        }
                        .promotion-highlight {
                            background-color: #ecf0f1;
                            border-left: 4px solid #27ae60;
                            padding: 15px;
                            margin: 20px 0;
                            font-weight: 500;
                        }
                        .compensation-section {
                            margin-top: 30px;
                            padding: 15px;
                            border: 1px solid #bdc3c7;
                            background-color: #f9f9f9;
                        }
                        .compensation-title {
                            font-weight: bold;
                            margin-bottom: 15px;
                            color: #2c3e50;
                            font-size: 14px;
                        }
                        .compensation-item {
                            margin: 8px 0;
                            font-size: 13px;
                            display: flex;
                            justify-content: space-between;
                        }
                        .signature-section {
                            margin-top: 50px;
                            display: flex;
                            justify-content: space-between;
                        }
                        .signature-box {
                            width: 40%;
                            border-top: 1px solid #333;
                            padding-top: 40px;
                            text-align: center;
                            font-size: 13px;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            font-size: 12px;
                            color: #666;
                            border-top: 1px solid #bdc3c7;
                            padding-top: 15px;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="organization-name">%s</div>
                        <div class="organization-details">
                            %s<br>
                            Email: %s | Phone: %s
                        </div>
                    </div>

                    <div class="date-section">
                        <strong>Date:</strong> %s
                    </div>

                    <div class="employee-details">
                        <strong>Employee ID:</strong> %s<br>
                        <strong>Department:</strong> %s
                    </div>

                    <div class="greeting">
                        Dear Employee,
                    </div>

                    <div class="content">
                        <p>
                            We are delighted to inform you of your promotion, which is a testament to your
                            outstanding performance, dedication, and valuable contributions to our organization.
                        </p>

                        <div class="promotion-highlight">
                            <strong>Promotion Details:</strong><br>
                            Previous Position: <strong>%s</strong><br>
                            New Position: <strong>%s</strong><br>
                            Effective Date: <strong>%s</strong>
                        </div>

                        <p>
                            This promotion recognizes your exceptional abilities, leadership qualities, and
                            commitment to excellence. We are confident that you will continue to demonstrate the
                            same level of professional excellence and dedication in your new role.
                        </p>

                        <p>
                            Your revised compensation details are outlined below:
                        </p>

                        <div class="compensation-section">
                            <div class="compensation-title">Revised Compensation Package</div>
                            <div class="compensation-item">
                                <span>Basic Salary:</span>
                                <span>%s</span>
                            </div>
                            <div class="compensation-item">
                                <span>House Rent Allowance (HRA):</span>
                                <span>%s</span>
                            </div>
                            <div class="compensation-item">
                                <span>Provident Fund (PF):</span>
                                <span>%s</span>
                            </div>
                            <div class="compensation-item">
                                <span>Net Monthly Pay:</span>
                                <span>%s</span>
                            </div>
                            <div class="compensation-item">
                                <span>Annual Package:</span>
                                <span>%s</span>
                            </div>
                        </div>

                        <p>
                            Please refer to the attached revised compensation card for complete details on your
                            benefits, bonuses, deductions, and other compensation components.
                        </p>

                        <p>
                            We wish you great success in your new position and look forward to your continued
                            contributions to our organization's growth and success.
                        </p>

                        <p>
                            Should you have any questions or require clarification regarding this promotion or
                            your new compensation package, please do not hesitate to contact the Human Resources
                            Department.
                        </p>

                        <p>
                            Once again, congratulations on this well-deserved promotion!
                        </p>
                    </div>

                    <div class="signature-section">
                        <div class="signature-box">
                            <strong>Employee Acknowledgment</strong><br>
                            Signature: ________________<br>
                            Date: ________________
                        </div>
                        <div class="signature-box">
                            <strong>HR Department</strong><br>
                            Signature: ________________<br>
                            Date: ________________
                        </div>
                    </div>

                    <div class="footer">
                        <p>
                            This is an official document from %s.<br>
                            Generated on: %s
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(
                templateData.getOrganizationName(),
                templateData.getOrganizationAddress(),
                templateData.getHrContactEmail(),
                templateData.getHrContactPhone(),
                currentDateStr,
                templateData.getEmployeeId(),
                templateData.getDepartment(),
                templateData.getPreviousTitle() != null ? templateData.getPreviousTitle() : "Senior Position",
                templateData.getTitle(),
                effectiveDateStr,
                formatCurrency(templateData.getBasePay()),
                formatCurrency(templateData.getHra()),
                formatCurrency(templateData.getPf()),
                formatCurrency(templateData.getNetPay()),
                formatCurrency(
                        templateData.getAnnualPackage() != null ? Double.parseDouble(templateData.getAnnualPackage())
                                : templateData.getGrossPay()),
                templateData.getOrganizationName(),
                currentDateStr);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Format currency amount
     */
    private String formatCurrency(Double amount) {
        if (amount == null) {
            return "₹0.00";
        }
        return CURRENCY_FORMAT.format(amount);
    }

    /**
     * Format timestamp to readable date string
     */
    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern(DATE_FORMAT));
        }
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        return formatter.format(timestamp);
    }
}
