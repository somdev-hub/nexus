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
                <body>
                    <p>Dear {name},</p>
                    <p>Welcome to Nexus Corporation! We are excited to have you join our team.</p>
                    <p>Your employee ID is: <strong>{employeeId}</strong></p>
                    <p>Please find attached the necessary documents to get you started.</p>
                    <p>If you have any questions, feel free to reach out to HR at hr@n
            exuscorporation.com.</p>
                    <p>Best regards,<br/>Nexus HR Team</p>
                </body>
                </html>
            """;
}
