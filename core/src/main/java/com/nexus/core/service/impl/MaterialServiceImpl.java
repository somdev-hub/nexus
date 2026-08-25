package com.nexus.core.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Material;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.MaterialDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.service.MaterialService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

	private final MaterialRepo materialRepo;
	private final ModelMapper modelMapper;

	@Override
	public ResponseEntity<?> addMaterial(MaterialDto materialDto) {
		Material material = modelMapper.map(materialDto, Material.class);
		Material savedMaterial = materialRepo.save(material);
		return new ResponseEntity<>(modelMapper.map(savedMaterial, MaterialDto.class), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getMaterialByIdAndOrg(Long id, Long orgId) {
		Material material = materialRepo.findByMaterialIdAndOrg(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", id));
		return new ResponseEntity<>(modelMapper.map(material, MaterialDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllMaterialsByOrgId(Long orgId, Pageable pageable) {
		Page<Material> materials = materialRepo.findByOrg(orgId, pageable);
		Page<MaterialDto> materialDtos = materials.map(m -> modelMapper.map(m, MaterialDto.class));
		return new ResponseEntity<>(materialDtos, HttpStatus.OK);
	}

}
