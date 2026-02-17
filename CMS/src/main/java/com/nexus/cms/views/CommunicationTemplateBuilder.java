package com.nexus.cms.views;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CommunicationTemplateBuilder {
    private static final String DATE_FORMAT = "dd-MM-yyyy";
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final TemplateEngine templateEngine;

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
     *
     * @param variables Map containing: employeeName, organizationName, position, department, appraisalDate,
     *                  effectiveDate, basePay, hra, netMonthlyPay, annualPackage, hrEmail
     * @return HTML email content
     */
    public String buildRewardAppraisalEmailTemplate(Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process("emails/reward-appraisal-email", context);
    }

}
