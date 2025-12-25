package com.nexus.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.MaterialDto;
import com.nexus.core.service.MaterialService;
import com.nexus.core.utils.CommonUtils;
import com.nexus.core.utils.Logger;

import io.micrometer.core.ipc.http.HttpSender;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/materials")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private Logger logger;

    @PostMapping("/add")
    public ResponseEntity<?> addMaterial(@RequestBody MaterialDto materialDto, Long org,
            @RequestHeader("Authorization") String token) {
        if (!commonUtils.validateToken(token)) {
            throw new InvalidCredentialsException();
        }
        ResponseEntity<?> response = null;
        try {
            response = materialService.addMaterial(materialDto, org);
        } catch (Exception e) {
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } finally {
            logger.log("/materials/add", HttpMethod.POST,
                    response != null ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR, materialDto,
                    response != null ? response.getBody() : null, org);
        }

        return response;

    }

}
