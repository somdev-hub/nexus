package com.nexus.iam.repository;

import com.nexus.iam.entities.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByResourceName(String resourceName);

    boolean existsByResourceName(String resourceName);
}
