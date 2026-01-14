package com.nexus.dms.service.impl;

import com.nexus.dms.dto.FolderListDto;
import com.nexus.dms.entities.FolderList;
import com.nexus.dms.entities.OrgType;
import com.nexus.dms.repository.FolderListRepo;
import com.nexus.dms.utils.CommonConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderListServiceImplTest {

    @Mock
    private FolderListRepo folderListRepo;

    @InjectMocks
    private FolderListServiceImpl folderListService;

    private List<FolderList> mockFolders;

    @BeforeEach
    void setUp() {
        mockFolders = new ArrayList<>();
        mockFolders.add(new FolderList(CommonConstants.RETAILER_FOLDER, "us-east-1", "Default User", OrgType.RETAILER));
        mockFolders.add(new FolderList(CommonConstants.SUPPLIER_FOLDER, "us-east-1", "Default User", OrgType.SUPPLIER));
    }

    @Test
    void testSetFolderListsNewFolders() {
        when(folderListRepo.existsByFolderName(anyString())).thenReturn(false);
        when(folderListRepo.save(any(FolderList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = folderListService.setFolderLists();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
    }

    @Test
    void testSetFolderListsSkipsExisting() {
        when(folderListRepo.existsByFolderName(CommonConstants.RETAILER_FOLDER)).thenReturn(true);
        when(folderListRepo.existsByFolderName(CommonConstants.SUPPLIER_FOLDER)).thenReturn(false);
        when(folderListRepo.existsByFolderName(CommonConstants.LOGISTICS_FOLDER)).thenReturn(false);
        when(folderListRepo.existsByFolderName(CommonConstants.COMMON_FOLDER)).thenReturn(false);
        when(folderListRepo.save(any(FolderList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = folderListService.setFolderLists();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> savedFolders = (List<?>) response.getBody();
        assertEquals(3, savedFolders.size());
    }

    @Test
    void testGetFolderListsSuccess() {
        when(folderListRepo.findAll()).thenReturn(mockFolders);

        ResponseEntity<?> response = folderListService.getFolderLists();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        List<?> folders = (List<?>) response.getBody();
        assertEquals(2, folders.size());
    }

    @Test
    void testGetFolderListsEmpty() {
        when(folderListRepo.findAll()).thenReturn(new ArrayList<>());

        ResponseEntity<?> response = folderListService.getFolderLists();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> folders = (List<?>) response.getBody();
        assertEquals(0, folders.size());
    }

    @Test
    void testGetFolderListsException() {
        when(folderListRepo.findAll()).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = folderListService.getFolderLists();

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertTrue(response.getBody() instanceof String);
        assertTrue(response.getBody().toString().contains("Error retrieving folder lists"));
    }

    @Test
    void testSetNewFolderListNotImplemented() {
        FolderListDto dto = new FolderListDto();

        assertThrows(UnsupportedOperationException.class, () -> {
            folderListService.setNewFolderList(dto);
        });
    }

    @Test
    void testDeleteFolderListNotImplemented() {
        assertThrows(UnsupportedOperationException.class, () -> {
            folderListService.deleteFolderList(1L);
        });
    }

}

