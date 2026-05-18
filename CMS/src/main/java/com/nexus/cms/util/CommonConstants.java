package com.nexus.cms.util;

import com.nexus.cms.chat.enums.AttachmentType;

import java.util.List;

public class CommonConstants {

    public static final String HR_INIT_EMAIL_FILE_NAME = "hr-init-email";
    public static final String PROMOTION_EMAIL_FILE_NAME = "promotion-email";
    public static final String REWARD_APPRAISAL_EMAIL_FILE_NAME = "reward-appraisal-email";
    public static final String SALARY_PAYMENT_EMAIL_FILE_NAME = "salary-payment-email";

    public static final String CANDIDATE_SELECTION_MAIL_TOPIC = "candidate-selection-mail-topic";
    public static final String CANDIDATE_REJECTION_MAIL_TOPIC = "candidate-rejection-mail-topic";
    public static final String CANDIDATE_PROMOTION_MAIL_TOPIC = "candidate-promotion-mail-topic";
    public static final String REWARD_APPRAISAL_MAIL_TOPIC = "reward-appraisal-mail-topic";
    public static final String SALARY_PAYMENT_MAIL_TOPIC = "salary-payment-mail-topic";

    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    public static final List<AttachmentType> IMAGE_AND_VIDEO_ATTACHMENT_TYPES = List.of(
            AttachmentType.JPG,
            AttachmentType.PNG,
            AttachmentType.GIF,
            AttachmentType.MP4,
            AttachmentType.MOV,
            AttachmentType.WAV
    );
    public static final List<AttachmentType> FILE_ATTACHMENT_TYPES = List.of(
            AttachmentType.PDF,
            AttachmentType.MP3,
            AttachmentType.DOCX,
            AttachmentType.XLSX,
            AttachmentType.PPTX,
            AttachmentType.ZIP,
            AttachmentType.OTHER
    );
}
