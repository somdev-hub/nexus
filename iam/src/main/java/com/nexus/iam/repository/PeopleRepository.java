package com.nexus.iam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.iam.entities.People;
import com.nexus.iam.entities.Role;

public interface PeopleRepository extends JpaRepository<People, Long> {

    List<People> findByRole(Role role);

    People findByUserId(Long userId);
}
