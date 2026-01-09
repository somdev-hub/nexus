package com.nexus.iam.repository;

import com.nexus.iam.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);

    Optional<User> findByEmail(String email);

    Boolean existsByName(String name);

    Boolean existsByEmail(String email);

    @Query(value = "SELECT u.id, u.name, u.email, u.password, u.phone, u.address, u.salary, u.joining_date, u.notes, u.created_at, u.profile_photo, u.enabled, u.account_non_expired, u.account_non_locked, u.credentials_non_expired, u.people_id " +
            "FROM iam.t_users u " +
            "INNER JOIN iam.t_people p ON u.people_id = p.id " +
            "WHERE p.organization_id = :orgId",
           countQuery = "SELECT count(u.id) FROM iam.t_users u " +
                   "INNER JOIN iam.t_people p ON u.people_id = p.id " +
                   "WHERE p.organization_id = :orgId",
           nativeQuery = true)
    Page<User> findByOrgId(Long orgId, Pageable pageable);
}
