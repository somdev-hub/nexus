package com.nexus.iam.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexus.iam.entities.People;
import com.nexus.iam.entities.Role;

public interface PeopleRepository extends JpaRepository<People, Long> {

    List<People> findByRole(Role role);

    @Query(value = "select * from iam.t_people p where p.user_id = :userId", nativeQuery = true)
    Optional<People> findByUserId(@Param("userId") Long userId);

}
