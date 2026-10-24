package com.nexus.cms.notification.service.implementations;

import com.nexus.cms.notification.entities.NotificationAddressToMaster;
import com.nexus.cms.notification.entities.NotificationAddressingTypeMaster;
import com.nexus.cms.notification.entities.NotificationEventTypeMaster;
import com.nexus.cms.notification.entities.NotificationFailureStatusMaster;
import com.nexus.cms.notification.entities.NotificationSeverityMaster;
import com.nexus.cms.notification.entities.NotificationMasterEntity;
import com.nexus.cms.notification.entities.NotificationPushTypeMaster;
import com.nexus.cms.notification.properties.NotificationMasterCodes;
import com.nexus.cms.notification.repositories.NotificationAddressToMasterRepo;
import com.nexus.cms.notification.repositories.NotificationAddressingTypeMasterRepo;
import com.nexus.cms.notification.repositories.NotificationEventTypeMasterRepo;
import com.nexus.cms.notification.repositories.NotificationFailureStatusMasterRepo;
import com.nexus.cms.notification.repositories.NotificationPushTypeMasterRepo;
import com.nexus.cms.notification.repositories.NotificationSeverityMasterRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Seeds the notification master tables on start-up.
 * <p>
 * The seeding is idempotent: a row that already exists is left alone, so
 * restarting the application never disturbs a catalogue that an administrator
 * has edited. The tables are the source of truth from then on — a new value
 * is an {@code INSERT}, and a value that should no longer be used is
 * deactivated by setting {@code is_active} to false.
 * <p>
 * The tables themselves are created by Hibernate, which the application
 * already has configured with {@code ddl-auto=update}, so no migration script
 * is needed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationMasterDataSeeder implements ApplicationRunner {

    private final NotificationEventTypeMasterRepo eventTypeRepo;
    private final NotificationSeverityMasterRepo severityRepo;
    private final NotificationAddressingTypeMasterRepo addressingTypeRepo;
    private final NotificationFailureStatusMasterRepo failureStatusRepo;
    private final NotificationAddressToMasterRepo addressToRepo;
    private final NotificationPushTypeMasterRepo pushTypeRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedEventTypes();
        seedSeverities();
        seedAddressingTypes();
        seedFailureStatuses();
        seedAddressTo();
        seedPushTypes();
        log.info("Notification master data seeded");
    }

    private void seedEventTypes() {
        seed(eventTypeRepo::findByNotificationEventType, eventTypeRepo::save,
                "notificationEventType", NotificationMasterCodes.EVENT_PURCHASE_ORDER_STATUS_CHANGED,
                "A purchase order changed status", NotificationEventTypeMaster::new,
                NotificationEventTypeMaster::setNotificationEventType);

        seed(eventTypeRepo::findByNotificationEventType, eventTypeRepo::save,
                "notificationEventType", NotificationMasterCodes.EVENT_QUOTATION_STATUS_CHANGED,
                "A quotation changed status", NotificationEventTypeMaster::new,
                NotificationEventTypeMaster::setNotificationEventType);

        seed(eventTypeRepo::findByNotificationEventType, eventTypeRepo::save,
                "notificationEventType", NotificationMasterCodes.EVENT_PARTNERSHIP_STATUS_CHANGED,
                "A partnership changed status", NotificationEventTypeMaster::new,
                NotificationEventTypeMaster::setNotificationEventType);

        seed(eventTypeRepo::findByNotificationEventType, eventTypeRepo::save,
                "notificationEventType", NotificationMasterCodes.EVENT_INVOICE_STATUS_CHANGED,
                "An invoice changed status", NotificationEventTypeMaster::new,
                NotificationEventTypeMaster::setNotificationEventType);

        seed(eventTypeRepo::findByNotificationEventType, eventTypeRepo::save,
                "notificationEventType", NotificationMasterCodes.EVENT_PAYMENT_STATUS_CHANGED,
                "A payment changed status", NotificationEventTypeMaster::new,
                NotificationEventTypeMaster::setNotificationEventType);

        seed(eventTypeRepo::findByNotificationEventType, eventTypeRepo::save,
                "notificationEventType", NotificationMasterCodes.EVENT_SHIPMENT_STATUS_CHANGED,
                "A shipment changed status (logistics)", NotificationEventTypeMaster::new,
                NotificationEventTypeMaster::setNotificationEventType);

        seed(eventTypeRepo::findByNotificationEventType, eventTypeRepo::save,
                "notificationEventType", NotificationMasterCodes.EVENT_GOODS_RECEIPT_STATUS_CHANGED,
                "A goods receipt changed status", NotificationEventTypeMaster::new,
                NotificationEventTypeMaster::setNotificationEventType);
    }

    private void seedSeverities() {
        seed(severityRepo::findByNotificationSeverity, severityRepo::save,
                "notificationSeverity", NotificationMasterCodes.SEVERITY_INFO,
                "Ordinary progress — an order was sent or acknowledged", NotificationSeverityMaster::new,
                NotificationSeverityMaster::setNotificationSeverity);

        seed(severityRepo::findByNotificationSeverity, severityRepo::save,
                "notificationSeverity", NotificationMasterCodes.SEVERITY_WARNING,
                "Something needs attention but nothing is broken", NotificationSeverityMaster::new,
                NotificationSeverityMaster::setNotificationSeverity);

        seed(severityRepo::findByNotificationSeverity, severityRepo::save,
                "notificationSeverity", NotificationMasterCodes.SEVERITY_CRITICAL,
                "Something is wrong", NotificationSeverityMaster::new,
                NotificationSeverityMaster::setNotificationSeverity);

        seed(severityRepo::findByNotificationSeverity, severityRepo::save,
                "notificationSeverity", NotificationMasterCodes.SEVERITY_ACTION_REQUIRED,
                "A specific person must act. Reserved for authority-based addressing",
                NotificationSeverityMaster::new, NotificationSeverityMaster::setNotificationSeverity);
    }

    private void seedAddressingTypes() {
        seed(addressingTypeRepo::findByNotificationAddressingType, addressingTypeRepo::save,
                "notificationAddressingType", NotificationMasterCodes.ADDRESSING_ORG,
                "Broadcast to every connected member of the target organisation",
                NotificationAddressingTypeMaster::new, NotificationAddressingTypeMaster::setNotificationAddressingType);

        seed(addressingTypeRepo::findByNotificationAddressingType, addressingTypeRepo::save,
                "notificationAddressingType", NotificationMasterCodes.ADDRESSING_USER,
                "Delivered to a named set of users", NotificationAddressingTypeMaster::new,
                NotificationAddressingTypeMaster::setNotificationAddressingType);

        seed(addressingTypeRepo::findByNotificationAddressingType, addressingTypeRepo::save,
                "notificationAddressingType", NotificationMasterCodes.ADDRESSING_AUTHORITY,
                "Delivered to the users holding a given authority within the organisation",
                NotificationAddressingTypeMaster::new, NotificationAddressingTypeMaster::setNotificationAddressingType);
    }

    private void seedFailureStatuses() {
        seed(failureStatusRepo::findByNotificationFailureStatus, failureStatusRepo::save,
                "notificationFailureStatus", NotificationMasterCodes.FAILURE_STATUS_FAILED,
                "The event could not be projected and is waiting to be replayed",
                NotificationFailureStatusMaster::new, NotificationFailureStatusMaster::setNotificationFailureStatus);

        seed(failureStatusRepo::findByNotificationFailureStatus, failureStatusRepo::save,
                "notificationFailureStatus", NotificationMasterCodes.FAILURE_STATUS_REPLAYED,
                "The event has been republished to the source topic", NotificationFailureStatusMaster::new,
                NotificationFailureStatusMaster::setNotificationFailureStatus);

        seed(failureStatusRepo::findByNotificationFailureStatus, failureStatusRepo::save,
                "notificationFailureStatus", NotificationMasterCodes.FAILURE_STATUS_PERMANENT,
                "The event has been replayed too many times and has been given up on",
                NotificationFailureStatusMaster::new, NotificationFailureStatusMaster::setNotificationFailureStatus);
    }

    private void seedAddressTo() {
        seed(addressToRepo::findByNotificationAddressTo, addressToRepo::save,
                "notificationAddressTo", NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY,
                "The organisation that did not perform the action", NotificationAddressToMaster::new,
                NotificationAddressToMaster::setNotificationAddressTo);

        seed(addressToRepo::findByNotificationAddressTo, addressToRepo::save,
                "notificationAddressTo", NotificationMasterCodes.ADDRESS_TO_ACTOR_ORG,
                "The organisation that performed the action", NotificationAddressToMaster::new,
                NotificationAddressToMaster::setNotificationAddressTo);

        seed(addressToRepo::findByNotificationAddressTo, addressToRepo::save,
                "notificationAddressTo", NotificationMasterCodes.ADDRESS_TO_RETAILER,
                "The organisation that raised the purchase order", NotificationAddressToMaster::new,
                NotificationAddressToMaster::setNotificationAddressTo);

        seed(addressToRepo::findByNotificationAddressTo, addressToRepo::save,
                "notificationAddressTo", NotificationMasterCodes.ADDRESS_TO_SUPPLIER,
                "The organisation that fulfils the purchase order", NotificationAddressToMaster::new,
                NotificationAddressToMaster::setNotificationAddressTo);
    }

    private void seedPushTypes() {
        seed(pushTypeRepo::findByNotificationPushType, pushTypeRepo::save,
                "notificationPushType", NotificationMasterCodes.PUSH_TYPE_MESSAGE,
                "A newly created notification", NotificationPushTypeMaster::new,
                NotificationPushTypeMaster::setNotificationPushType);

        seed(pushTypeRepo::findByNotificationPushType, pushTypeRepo::save,
                "notificationPushType", NotificationMasterCodes.PUSH_TYPE_SNAPSHOT,
                "The current inbox snapshot and unread count", NotificationPushTypeMaster::new,
                NotificationPushTypeMaster::setNotificationPushType);

        seed(pushTypeRepo::findByNotificationPushType, pushTypeRepo::save,
                "notificationPushType", NotificationMasterCodes.PUSH_TYPE_ACK,
                "Acknowledgement of a read-state change", NotificationPushTypeMaster::new,
                NotificationPushTypeMaster::setNotificationPushType);
    }

    /**
     * Insert a catalogue row if it is not there yet.
     * <p>
     * An existing row is left exactly as it is: an administrator may have
     * changed its description or deactivated it, and seeding must not undo
     * that.
     */
    private <T extends NotificationMasterEntity> void seed(
            Function<String, Optional<T>> findByCode,
            Function<T, T> save,
            String label,
            String code,
            String description,
            Supplier<T> factory,
            BiConsumer<T, String> setCode) {

        if (findByCode.apply(code).isPresent()) {
            return;
        }
        T row = factory.get();
        setCode.accept(row, code);
        row.setDescription(description);
        row.setIsActive(true);
        row.setCreatedAt(Timestamp.from(Instant.now()));
        row.setCreatedBy("notification-master-seeder");
        row.setUpdatedAt(Timestamp.from(Instant.now()));
        save.apply(row);
        log.info("Seeded notification master {} = {}", label, code);
    }
}
