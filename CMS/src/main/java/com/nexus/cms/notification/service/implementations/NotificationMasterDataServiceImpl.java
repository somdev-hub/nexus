package com.nexus.cms.notification.service.implementations;

import com.nexus.cms.notification.entities.NotificationMasterEntity;
import com.nexus.cms.notification.repositories.NotificationAddressToMasterRepo;
import com.nexus.cms.notification.repositories.NotificationAddressingTypeMasterRepo;
import com.nexus.cms.notification.repositories.NotificationEventTypeMasterRepo;
import com.nexus.cms.notification.repositories.NotificationFailureStatusMasterRepo;
import com.nexus.cms.notification.repositories.NotificationPushTypeMasterRepo;
import com.nexus.cms.notification.repositories.NotificationSeverityMasterRepo;
import com.nexus.cms.notification.service.interfaces.NotificationMasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Reads the notification catalogue from the master tables.
 * <p>
 * The catalogue is consulted on the event path, so it must not add a query
 * per event. The lookups are therefore direct repository calls by unique
 * code, and a value that is not in the catalogue is reported and treated as
 * not allowed — a missing value must never be assumed to be permitted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationMasterDataServiceImpl implements NotificationMasterDataService {

    private final NotificationEventTypeMasterRepo eventTypeRepo;
    private final NotificationSeverityMasterRepo severityRepo;
    private final NotificationAddressingTypeMasterRepo addressingTypeRepo;
    private final NotificationFailureStatusMasterRepo failureStatusRepo;
    private final NotificationAddressToMasterRepo addressToRepo;
    private final NotificationPushTypeMasterRepo pushTypeRepo;

    @Override
    public boolean isActiveEventType(String code) {
        return isActive(eventTypeRepo::findByNotificationEventType, code, "Event type");
    }

    @Override
    public boolean isActiveSeverity(String code) {
        return isActive(severityRepo::findByNotificationSeverity, code, "Severity");
    }

    @Override
    public boolean isActiveAddressingType(String code) {
        return isActive(addressingTypeRepo::findByNotificationAddressingType, code, "Addressing type");
    }

    @Override
    public boolean isActiveAddressTo(String code) {
        return isActive(addressToRepo::findByNotificationAddressTo, code, "Address-to");
    }

    @Override
    public boolean isActiveFailureStatus(String code) {
        return isActive(failureStatusRepo::findByNotificationFailureStatus, code, "Failure status");
    }

    @Override
    public boolean isActivePushType(String code) {
        return isActive(pushTypeRepo::findByNotificationPushType, code, "Push type");
    }

    @Override
    public Set<String> activeEventTypes() {
        Set<String> codes = new HashSet<>();
        eventTypeRepo.findByIsActiveTrue().forEach(row -> codes.add(row.getNotificationEventType()));
        return codes;
    }

    /**
     * Whether a catalogue row exists and is active.
     * <p>
     * A value that is not in the catalogue is not allowed. Failing closed
     * matters here: a typo in a code must not silently allow something that
     * was never meant to be used.
     */
    private <T extends NotificationMasterEntity> boolean isActive(
            Function<String, Optional<T>> findByCode, String code, String label) {
        if (code == null) {
            return false;
        }
        return findByCode.apply(code)
                .map(row -> Boolean.TRUE.equals(row.getIsActive()))
                .orElseGet(() -> {
                    log.warn("{} '{}' is not in the master table — it will not be used", label, code);
                    return false;
                });
    }
}
