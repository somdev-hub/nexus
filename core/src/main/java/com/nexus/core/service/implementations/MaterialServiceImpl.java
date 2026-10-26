package com.nexus.core.service.implementations;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.model.entities.Material;
import com.nexus.core.model.entities.SupplierCatalog;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.MaterialDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.service.interfaces.MaterialService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

	private final MaterialRepo materialRepo;
	private final SupplierCatalogRepo supplierCatalogRepo;
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
		for (Material m : materials.getContent()) {
			backfillMaterialDetails(m);
		}
		Page<MaterialDto> materialDtos = materials.map(m -> modelMapper.map(m, MaterialDto.class));
		return new ResponseEntity<>(materialDtos, HttpStatus.OK);
	}

	private void backfillMaterialDetails(Material m) {
		boolean changed = false;
		if (m.getCategory() == null && m.getCatalogId() != null) {
			SupplierCatalog catalog = null;
			if (m.getSupplierOrgId() != null) {
				catalog = supplierCatalogRepo.findByCatalogIdAndSupplierOrgAccountId(m.getCatalogId(), m.getSupplierOrgId())
						.orElse(null);
			}
			if (catalog == null) {
				catalog = supplierCatalogRepo.findById(m.getCatalogId()).orElse(null);
			}
			if (catalog != null) {
				m.setCategory(catalog.getCategory() != null ? catalog.getCategory() : catalog.getFamily());
				if (m.getName() == null) m.setName(catalog.getName());
				if (m.getCode() == null) m.setCode(catalog.getCode());
				if (m.getUnit() == null) m.setUnit(catalog.getUnitOfMeasure());
				if (m.getCurrency() == null) m.setCurrency(catalog.getCurrency() != null ? catalog.getCurrency() : "USD");
				if (m.getPricePerUnit() == null) m.setPricePerUnit(catalog.getBasePrice());
				changed = true;
			}
		}
		if (changed) {
			materialRepo.save(m);
		}
	}

}
