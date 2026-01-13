package com.nexus.dms.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.dms.dto.FolderListDto;
import com.nexus.dms.entities.FolderList;
import com.nexus.dms.entities.OrgType;
import com.nexus.dms.repository.FolderListRepo;
import com.nexus.dms.service.FolderListService;
import com.nexus.dms.utils.CommonConstants;

@Service
public class FolderListServiceImpl implements FolderListService {

    @Autowired
    private FolderListRepo folderListRepo;

    @Override
    public ResponseEntity<?> setFolderLists() {
        List<FolderList> folders = List.of(
                new FolderList(CommonConstants.RETAILER_FOLDER, "us-east-1", "Default User", OrgType.RETAILER),
                new FolderList(CommonConstants.SUPPLIER_FOLDER, "us-east-1", "Default User", OrgType.SUPPLIER),
                new FolderList(CommonConstants.LOGISTICS_FOLDER, "us-east-1", "Default User", OrgType.LOGISTICS),
                new FolderList(CommonConstants.COMMON_FOLDER, "us-east-1", "Default User", OrgType.COMMON));

        List<FolderList> savedFolders = new ArrayList<>();
        for (FolderList folder : folders) {
            if (!folderListRepo.existsByFolderName(folder.getFolderName())) {
                savedFolders.add(folderListRepo.save(folder));
            }
        }

        return ResponseEntity.ok(savedFolders);
    }

    @Override
    public ResponseEntity<?> getFolderLists() {
        ResponseEntity<?> response = null;
        try {
            List<FolderList> folderLists = folderListRepo.findAll();
            response = ResponseEntity.ok(folderLists);
        } catch (Exception e) {
            response = ResponseEntity.status(500).body("Error retrieving folder lists: " + e.getMessage());
        }
        return response;
    }

    @Override
    public ResponseEntity<?> setNewFolderList(FolderListDto folderListDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setNewFolderList'");
    }

    @Override
    public ResponseEntity<?> deleteFolderList(Long folderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteFolderList'");
    }

}
