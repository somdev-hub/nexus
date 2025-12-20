package com.nexus.iam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.service.PeopleService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/iam/people")
public class PeopleController {

    @Autowired
    PeopleService peopleService;

    @PostMapping("/create")
    public ResponseEntity<?> createPeople(@RequestBody Long userId, @RequestBody Long roleId) {
        peopleService.createPeople(userId, roleId);
        return ResponseEntity.ok("People created successfully");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deletePeopleByUserId(@RequestBody Long userId) {
        peopleService.deletePeopleByUserId(userId);
        return ResponseEntity.ok("People deleted successfully");
    }

}
