package com.nexus.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Material;

@Repository
public interface MaterialRepo extends JpaRepository<Material, Long> {

}
