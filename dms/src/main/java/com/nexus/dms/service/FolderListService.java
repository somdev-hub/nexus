package com.nexus.dms.service;

import org.springframework.http.ResponseEntity;

import com.nexus.dms.dto.FolderListDto;

public interface FolderListService {
    ResponseEntity<?> setFolderLists();

    ResponseEntity<?> getFolderLists();

    ResponseEntity<?> setNewFolderList(FolderListDto folderListDto);

    ResponseEntity<?> deleteFolderList(Long folderId);
}
