package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.GrantPermissionDto;
import com.nexus.iam.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/iam/permissions")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PermissionController {

    private final PermissionService permissionService;

    @LogActivity("Grant Permission to Role")
    @PostMapping(value = "/grant")
    public ResponseEntity<?> grantPermissionToRole(@RequestBody GrantPermissionDto grantPermissionDto) {
        return permissionService.grantPermission(grantPermissionDto);
    }
}
