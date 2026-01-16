package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import com.nexus.iam.dto.UserProfileDto;
import com.nexus.iam.service.UserService;

@RestController
@RequestMapping("/iam/users")
public class UserController {

    @Autowired
    private UserService userService;

    @LogActivity("Add User")
    @PostMapping("/add")
    public ResponseEntity<?> addUser(@RequestBody UserProfileDto user) {

        if (ObjectUtils.isEmpty(user)) {
            return new ResponseEntity<>("Request body must not be null", HttpStatus.BAD_REQUEST);
        }

        return userService.createUser(user);
    }

    @LogActivity("Get All Employees")
    @GetMapping("/employees")
    public ResponseEntity<?> getAllEmployees(@RequestParam(value = "orgId", required = true) Long orgId,
                                             @RequestParam(
                                                     value = "page", defaultValue = "0", required = false
                                             ) Integer page,
                                             @RequestParam(value = "pageOffset", defaultValue = "10", required = false) Integer pageOffset
    ) {
        if (ObjectUtils.isEmpty(orgId)) {
            return new ResponseEntity<>("Org id must not be null", HttpStatus.BAD_REQUEST);
        }

        return userService.getAllEmployees(orgId, page, pageOffset);
    }

}
