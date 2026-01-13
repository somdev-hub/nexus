package com.nexus.dms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.dms.entities.FolderList;

public interface FolderListRepo extends JpaRepository<FolderList, Long> {

    boolean existsByFolderName(String folderName);

    Optional<FolderList> findByFolderName(String folderName);
}
