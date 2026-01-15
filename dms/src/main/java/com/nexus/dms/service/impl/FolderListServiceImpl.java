package com.nexus.dms.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
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

    private final FolderListRepo folderListRepo;

    private final ModelMapper modelMapper;

    public FolderListServiceImpl(FolderListRepo folderListRepo, ModelMapper modelMapper) {
        this.folderListRepo = folderListRepo;
        this.modelMapper = modelMapper;
    }

    @Override
    public ResponseEntity<?> setFolderLists() {

        List<FolderList> folders = List.of(
                new FolderList(CommonConstants.RETAILER_FOLDER, CommonConstants.US_EAST_1, CommonConstants.DEFAULT_USER, OrgType.RETAILER),
                new FolderList(CommonConstants.SUPPLIER_FOLDER, CommonConstants.US_EAST_1, CommonConstants.DEFAULT_USER, OrgType.SUPPLIER),
                new FolderList(CommonConstants.LOGISTICS_FOLDER, CommonConstants.US_EAST_1, CommonConstants.DEFAULT_USER, OrgType.LOGISTICS),
                new FolderList(CommonConstants.COMMON_FOLDER, CommonConstants.US_EAST_1, CommonConstants.DEFAULT_USER, OrgType.COMMON));

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
            List<FolderListDto> folderListDtos = folderLists.stream().map(folderList -> modelMapper.map(folderList, FolderListDto.class)).toList();
            response = ResponseEntity.ok(folderListDtos);
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
