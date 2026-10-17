package com.nexus.iam.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.iam.entities.UserPreference;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    /** All active preferences for a user within a setting group. */
    List<UserPreference> findByUserIdAndSettingNameAndIsActiveTrue(Long userId, String settingName);

    /** A single preference row (active or not) for upsert lookups. */
    Optional<UserPreference> findByUserIdAndSettingNameAndKey(Long userId, String settingName, String key);

    /** Batch lookup used to upsert several keys without N+1 queries. */
    List<UserPreference> findByUserIdAndSettingNameAndKeyIn(Long userId, String settingName, List<String> keys);
}
