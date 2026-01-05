package com.nexus.iam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.dto.UserProfileDto;
import com.nexus.iam.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/iam/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public ResponseEntity<?> addUser(@RequestBody UserProfileDto user) {

        if (ObjectUtils.isEmpty(user)) {
            return new ResponseEntity<>("Request body must not be null", HttpStatus.BAD_REQUEST);
        }

        return userService.createUser(user);
    }

}
