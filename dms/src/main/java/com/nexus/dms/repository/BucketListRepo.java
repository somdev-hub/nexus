package com.nexus.dms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.dms.entities.BucketList;

public interface BucketListRepo extends JpaRepository<BucketList, Long> {

    boolean existsByBucketName(String bucketName);

    Optional<BucketList> findByBucketName(String bucketName);
}
