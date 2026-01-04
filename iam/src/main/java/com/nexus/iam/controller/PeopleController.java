package com.nexus.iam.controller;

import java.util.Map;

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
    public ResponseEntity<?> createPeople(@RequestBody Map<String, String> payload) {
        Long userId = Long.parseLong(payload.get("userId"));
        String role = payload.get("role");
        return peopleService.createPeople(userId, role);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deletePeopleByUserId(@RequestBody Long userId) {
        peopleService.deletePeopleByUserId(userId);
        return ResponseEntity.ok("People deleted successfully");
    }

}
