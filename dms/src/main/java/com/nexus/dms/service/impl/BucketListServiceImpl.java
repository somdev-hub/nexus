package com.nexus.dms.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.dms.dto.BucketListDto;
import com.nexus.dms.entities.BucketList;
import com.nexus.dms.entities.OrgType;
import com.nexus.dms.repository.BucketListRepo;
import com.nexus.dms.service.BucketListService;
import com.nexus.dms.utils.CommonConstants;

@Service
public class BucketListServiceImpl implements BucketListService {

    @Autowired
    private BucketListRepo bucketListRepo;

    @Override
    public ResponseEntity<?> setBucketLists() {
        List<BucketList> buckets = List.of(
                new BucketList(CommonConstants.RETAILER_BUCKET, "mumbai", "Default User", OrgType.RETAILER),
                new BucketList(CommonConstants.SUPPLIER_BUCKET, "mumbai", "Default User", OrgType.SUPPLIER),
                new BucketList(CommonConstants.LOGISTICS, "mumbai", "Default User", OrgType.LOGISTICS),
                new BucketList(CommonConstants.COMMON_BUCKET, "mumbai", "Default User", OrgType.COMMON));

        List<BucketList> savedBuckets = new ArrayList<>();
        for (BucketList bucket : buckets) {
            if (!bucketListRepo.existsByBucketName(bucket.getBucketName())) {
                savedBuckets.add(bucketListRepo.save(bucket));
            }
        }

        return ResponseEntity.ok(savedBuckets);
    }

    @Override
    public ResponseEntity<?> getBucketLists() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBucketLists'");
    }

    @Override
    public ResponseEntity<?> setNewBucketList(BucketListDto bucketListDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setNewBucketList'");
    }

    @Override
    public ResponseEntity<?> deleteBucketList(Long bucketId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteBucketList'");
    }

}
