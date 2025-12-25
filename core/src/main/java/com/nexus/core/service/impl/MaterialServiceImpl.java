package com.nexus.core.service.impl;

import java.sql.Timestamp;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.core.entities.Material;
import com.nexus.core.payload.ErrorResponse;
import com.nexus.core.payload.MaterialDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.service.MaterialService;

@Service
public class MaterialServiceImpl implements MaterialService {

    @Autowired
    private MaterialRepo materialRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<?> addMaterial(MaterialDto materialDto, Long org) {
        if (ObjectUtils.isEmpty(materialDto) || ObjectUtils.isEmpty(org)) {
            return new ResponseEntity<ErrorResponse>(
                    new ErrorResponse("Empty Details sent", HttpStatus.BAD_REQUEST.value(),
                            new Timestamp(System.currentTimeMillis()), "Necessary details are not sent!"),
                    HttpStatus.BAD_REQUEST);

        }
        try {

            Material material = modelMapper.map(materialDto, Material.class);
            material.setOrg(org);
            Material savedMaterial = materialRepo.save(material);
            return new ResponseEntity<>(modelMapper.map(savedMaterial, MaterialDto.class), HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<ErrorResponse>(
                    new ErrorResponse("Failed to add material", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            new Timestamp(System.currentTimeMillis()), e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
