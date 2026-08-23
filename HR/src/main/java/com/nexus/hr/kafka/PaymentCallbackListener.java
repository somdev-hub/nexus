package com.nexus.hr.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.hr.payload.PayrollCallbackDto;
import com.nexus.hr.service.interfaces.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Kafka Consumer Listener for Payment Callbacks
 * 
 * Listens to the payment-callback-topic and processes payment completion
 * notifications
 * from the PMS (Payment Management Service) microservice.
 * 
 * Replaces the previous HTTP callback mechanism with asynchronous Kafka
 * messaging.
 * 
 * Flow:
 * 1. PMS completes payment processing
 * 2. PMS publishes callback message to Kafka payment-callback-topic
 * 3. This listener consumes the message
 * 4. Extracts payroll callback data
 * 5. Calls PayrollService.handlePayrollCallback to process the payment
 * completion
 * 6. Triggers payslip generation and email notification
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackListener {

    private final PayrollService payrollService;
    private final ObjectMapper objectMapper;

    /**
     * Listen to payment callback events from PMS via Kafka.
     * 
     * Topic: payment-callback-topic
     * Consumer Group: hr-payment-callback-group
     * 
     * Message format (JSON array):
     * [
     * {
     * "payrollId": 123,
     * "paymentReferenceId": "SALARY_PAYMENT_APRIL_2026_uuid",
     * "success": true
     * },
     * ...
     * ]
     * 
     * @param payload    Raw message payload as JSON string
     * @param messageKey Message key (format: "payment-{paymentId}")
     * @param partition  Kafka partition
     * @param offset     Kafka offset
     */
    @KafkaListener(topics = "payment-callback-topic", groupId = "hr-payment-callback-group", containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentCallback(
            @Payload String payload,
            @Header("kafka_receivedMessageKey") String messageKey,
            @Header("kafka_receivedPartitionId") int partition,
            @Header("kafka_offset") long offset,
            Acknowledgment acknowledgment) {
        try {
            log.info("=== RECEIVED PAYMENT CALLBACK ===");
            log.info("Message Key: {}, Partition: {}, Offset: {}", messageKey, partition, offset);
            log.info("Payload: {}", payload);

            // Parse JSON payload to extract callback DTOs
            PayrollCallbackDto[] callbackDtos = objectMapper.readValue(payload, PayrollCallbackDto[].class);
            List<PayrollCallbackDto> callbackList = Arrays.asList(callbackDtos);

            log.info("Parsed {} payroll callback records from Kafka message", callbackList.size());

            // Log callback details for debugging
            for (PayrollCallbackDto callback : callbackList) {
                log.info("Processing Callback - PayrollId: {}, PaymentRef: {}, Success: {}",
                        callback.getPayrollId(),
                        callback.getPaymentReferenceId(),
                        callback.getSuccess());
            }

            // Delegate to PayrollService to handle the callback
            // This maintains the same processing logic as the HTTP callback endpoint
            payrollService.handlePayrollCallback(callbackList);

            log.info("Payment callback processed successfully from Kafka for message key: {}", messageKey);

            // Acknowledge message ONLY after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.info("Message acknowledged successfully. Partition: {}, Offset: {}", partition, offset);
            }

        } catch (Exception e) {
            log.error("Error processing payment callback from Kafka for message key: {}", messageKey, e);
            // DO NOT acknowledge on error - let Kafka re-deliver this message
            // This ensures the message is not lost on error
            throw new RuntimeException("Failed to process payment callback. Message will be retried.", e);
        }
    }
}
