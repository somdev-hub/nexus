package com.nexus.dms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.dms.entities.BucketList;

public interface BucketListRepo extends JpaRepository<BucketList, Long> {

}
