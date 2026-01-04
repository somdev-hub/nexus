package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface PeopleService {

    public ResponseEntity<?> createPeople(Long userId, String role);

    public void deletePeopleByUserId(Long userId);

}
