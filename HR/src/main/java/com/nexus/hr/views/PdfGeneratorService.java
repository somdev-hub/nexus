package com.nexus.hr.views;

import com.itextpdf.html2pdf.HtmlConverter;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.payload.PdfTemplateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service for generating PDF documents from HTML templates
 */
@Slf4j
@Service
public class PdfGeneratorService {

    private final CommunicationTemplateBuilder communicationTemplateBuilder;

    public PdfGeneratorService(CommunicationTemplateBuilder communicationTemplateBuilder) {
        this.communicationTemplateBuilder = communicationTemplateBuilder;
    }

    /**
     * Generate a joining letter PDF
     *
     * @param templateData Data to populate the template
     * @return PDF as MultipartFile
     */
    public MultipartFile generateJoiningLetterPdf(PdfTemplateDto templateData) {
        try {
            log.info("Generating joining letter PDF for employee ID: {}", templateData.getEmployeeId());

            String htmlContent = communicationTemplateBuilder.buildJoiningLetterTemplate(templateData);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            HtmlConverter.convertToPdf(htmlContent, outputStream);

            log.info("Joining letter PDF generated successfully for employee ID: {}", templateData.getEmployeeId());
            byte[] byteArray = outputStream.toByteArray();

            String fileName = "Joining_Letter_" + templateData.getEmployeeId() + ".pdf";
            return convertToMultipartFile(byteArray, fileName);

        } catch (Exception e) {
            log.error("Error generating joining letter PDF for employee ID: {}", templateData.getEmployeeId(), e);
            throw new ServiceLevelException(
                    "PDF Generator Service",
                    "Error generating joining letter PDF",
                    "generateJoiningLetterPdf",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    /**
     * Generate a letter of intent PDF
     *
     * @param templateData Data to populate the template
     * @return PDF as MultipartFile
     */
    public MultipartFile generateLetterOfIntentPdf(PdfTemplateDto templateData) {
        try {
            log.info("Generating letter of intent PDF for employee ID: {}", templateData.getEmployeeId());

            String htmlContent = communicationTemplateBuilder.buildLetterOfIntentTemplate(templateData);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            HtmlConverter.convertToPdf(htmlContent, outputStream);

            log.info("Letter of intent PDF generated successfully for employee ID: {}", templateData.getEmployeeId());
            byte[] byteArray = outputStream.toByteArray();

            String fileName = "Letter_Of_Intent_" + templateData.getEmployeeId() + ".pdf";
            return convertToMultipartFile(byteArray, fileName);

        } catch (Exception e) {
            log.error("Error generating letter of intent PDF for employee ID: {}", templateData.getEmployeeId(), e);
            throw new ServiceLevelException(
                    "PDF Generator Service",
                    "Error generating letter of intent PDF",
                    "generateLetterOfIntentPdf",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    /**
     * Generate a compensation card PDF
     *
     * @param templateData Data to populate the template
     * @return PDF as MultipartFile
     */
    public MultipartFile generateCompensationCardPdf(PdfTemplateDto templateData) {
        try {
            log.info("Generating compensation card PDF for employee ID: {}", templateData.getEmployeeId());

            String htmlContent = communicationTemplateBuilder.buildCompensationCardTemplate(templateData);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            HtmlConverter.convertToPdf(htmlContent, outputStream);

            log.info("Compensation card PDF generated successfully for employee ID: {}", templateData.getEmployeeId());
            byte[] byteArray = outputStream.toByteArray();

            String fileName = "Compensation_Card_" + templateData.getEmployeeId() + ".pdf";
            return convertToMultipartFile(byteArray, fileName);

        } catch (Exception e) {
            log.error("Error generating compensation card PDF for employee ID: {}", templateData.getEmployeeId(), e);
            throw new ServiceLevelException(
                    "PDF Generator Service",
                    "Error generating compensation card PDF",
                    "generateCompensationCardPdf",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    /**
     * Generate a promotion letter PDF
     *
     * @param templateData Data to populate the template
     * @return PDF as MultipartFile
     */
    public MultipartFile generatePromotionLetterPdf(PdfTemplateDto templateData) {
        try {
            log.info("Generating promotion letter PDF for employee ID: {}", templateData.getEmployeeId());

            String htmlContent = communicationTemplateBuilder.buildPromotionLetterTemplate(templateData);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            HtmlConverter.convertToPdf(htmlContent, outputStream);

            log.info("Promotion letter PDF generated successfully for employee ID: {}", templateData.getEmployeeId());
            byte[] byteArray = outputStream.toByteArray();

            String fileName = "Promotion_Letter_" + templateData.getEmployeeId() + ".pdf";
            return convertToMultipartFile(byteArray, fileName);

        } catch (Exception e) {
            log.error("Error generating promotion letter PDF for employee ID: {}", templateData.getEmployeeId(), e);
            throw new ServiceLevelException(
                    "PDF Generator Service",
                    "Error generating promotion letter PDF",
                    "generatePromotionLetterPdf",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    /**
     * Convert byte array to MultipartFile
     *
     * @param bytes    PDF content as byte array
     * @param fileName Name of the PDF file
     * @return MultipartFile containing the PDF
     */
    private MultipartFile convertToMultipartFile(byte[] bytes, String fileName) {
        return new MockMultipartFile(
                "file",
                fileName,
                "application/pdf",
                bytes);
    }
}
