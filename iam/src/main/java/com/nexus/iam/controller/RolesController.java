package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.exception.UnauthorizedException;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iam/roles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RolesController {

    private final RoleService roleService;
    private final JwtUtil jwtUtil;

    @LogActivity("Create All Roles")
    @GetMapping("/create/roles")
    public ResponseEntity<?> createRolesInDb() {
        roleService.initializeRoles();
        return new ResponseEntity<>("Roles created successfully", HttpStatus.OK);
    }

    @LogActivity("Create Single Role")
    @PostMapping("/create/role")
    public ResponseEntity<?> createRole(@RequestParam String role, @RequestParam Long deptId, @RequestHeader("Authorization") String authHeader) {
        if (ObjectUtils.isEmpty(authHeader) || !jwtUtil.isValidToken(authHeader)) {
            throw new UnauthorizedException(
                    "Unauthorized! Please use credentials",
                    "Unable to validate token"
            );
        }
        roleService.createRoleIfNotFound(role, deptId, authHeader);
        return new ResponseEntity<>("Role created successfully", HttpStatus.OK);
    }

    @LogActivity("Delete Role")
    @DeleteMapping("/delete/role")
    public ResponseEntity<?> deleteRole(@RequestParam String role) {
        roleService.deleteRoleByName(role);
        return new ResponseEntity<>("Role deleted successfully", HttpStatus.OK);
    }

}
