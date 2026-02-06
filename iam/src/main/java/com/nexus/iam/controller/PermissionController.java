package com.nexus.iam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.GrantPermissionDto;
import com.nexus.iam.service.PermissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @LogActivity("Grant Permission")
    @PostMapping("/add")
    public ResponseEntity<?> addPermission(@RequestBody GrantPermissionDto dto) {
        return permissionService.grantPermission(dto);
    }

}
