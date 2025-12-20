package com.nexus.iam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.service.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/iam/roles")
@CrossOrigin(origins = "*")
public class RolesController {

    @Autowired
    private RoleService roleService;

    @GetMapping("/create/roles")
    public ResponseEntity<?> createRolesInDb() {
        roleService.initializeRoles();
        return new ResponseEntity<>("Roles created successfully", HttpStatus.OK);
    }

    @GetMapping("/create/role")
    public ResponseEntity<?> createRole(@RequestParam String role) {
        roleService.createRoleIfNotFound(role);
        return new ResponseEntity<>("Role created successfully", HttpStatus.OK);
    }

    @DeleteMapping("/delete/role")
    public ResponseEntity<?> deleteRole(@RequestParam String role) {
        roleService.deleteRoleByName(role);
        return new ResponseEntity<>("Role deleted successfully", HttpStatus.OK);
    }

}
