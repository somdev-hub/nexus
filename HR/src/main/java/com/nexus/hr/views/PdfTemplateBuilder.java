package com.nexus.hr.views;

import com.nexus.hr.payload.PdfTemplateDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Template builder for generating HTML templates for PDF conversion
 */
@Component
public class PdfTemplateBuilder {

    private static final String DATE_FORMAT = "dd-MM-yyyy";

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
                            width: 100%;
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
                            width: 30%;
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
                            <div style="margin-top: 5px;">
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
                """.formatted(
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
                templateData.getHrContactPhone()
        );
    }

    /**
     * Generate a letter of intent HTML template
     */
    public String buildLetterOfIntentTemplate(PdfTemplateDto templateData) {
        String effectiveDateStr = formatDate(templateData.getEffectiveFrom());
        String currentDate = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern(DATE_FORMAT));

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
                            width: 45%;
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
                            Your compensation and other benefits will be communicated separately. This Letter of Intent represents 
                            a binding commitment from both parties and supersedes any previous offers or discussions.
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
                """.formatted(
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
                templateData.getEmployeeName(),
                templateData.getOrganizationName(),
                templateData.getHrContactEmail(),
                templateData.getHrContactPhone(),
                templateData.getOrganizationAddress()
        );
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
