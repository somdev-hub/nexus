package com.nexus.hr.utils;

public class CommonConstants {

    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String X_WWW_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String POST = "POST";


    public static final String HR_INIT_EMAIL_TEMPLATE = """
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
