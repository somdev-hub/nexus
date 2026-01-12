package com.nexus.dms.service;

import org.springframework.http.ResponseEntity;

import com.nexus.dms.dto.BucketListDto;

public interface BucketListService {
    ResponseEntity<?> setBucketLists();

    ResponseEntity<?> getBucketLists();

    ResponseEntity<?> setNewBucketList(BucketListDto bucketListDto);

    ResponseEntity<?> deleteBucketList(Long bucketId);
}
