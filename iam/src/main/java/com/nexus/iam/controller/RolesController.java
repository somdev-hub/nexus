package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iam/roles")
@CrossOrigin(origins = "*")
public class RolesController {

    private final RoleService roleService;

    public RolesController(RoleService roleService) {
        this.roleService = roleService;
    }

    @LogActivity("Create All Roles")
    @GetMapping("/create/roles")
    public ResponseEntity<?> createRolesInDb() {
        roleService.initializeRoles();
        return new ResponseEntity<>("Roles created successfully", HttpStatus.OK);
    }

    @LogActivity("Create Single Role")
    @GetMapping("/create/role")
    public ResponseEntity<?> createRole(@RequestParam String role) {
        roleService.createRoleIfNotFound(role);
        return new ResponseEntity<>("Role created successfully", HttpStatus.OK);
    }

    @LogActivity("Delete Role")
    @DeleteMapping("/delete/role")
    public ResponseEntity<?> deleteRole(@RequestParam String role) {
        roleService.deleteRoleByName(role);
        return new ResponseEntity<>("Role deleted successfully", HttpStatus.OK);
    }

}
