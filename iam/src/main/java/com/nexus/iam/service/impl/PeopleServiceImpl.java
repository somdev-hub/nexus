package com.nexus.iam.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.iam.entities.People;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.repository.PeopleRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.PeopleService;

@Service
public class PeopleServiceImpl implements PeopleService {

    @Autowired
    private PeopleRepository peopleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public ResponseEntity<?> createPeople(Long userId, String role) {
        try {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            var roleEntity = roleRepository.findByName(role)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", role));

            People people = new People();
            people.setUser(user);
            people.setRole(roleEntity);

            // Add role to user's roles set for JWT claims
            user.getRoles().add(roleEntity);

            peopleRepository.save(people);
            userRepository.save(user);

            // return the role
            return ResponseEntity.ok(Map.of("role", "ROLE_" + roleEntity.getName()));

        } catch (Exception e) {
            throw new RuntimeException("Error creating people: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePeopleByUserId(Long userId) {
        try {
            People people = peopleRepository.findByUserId(userId).orElseThrow(() -> {
                throw new ResourceNotFoundException("People", "userId", userId);
            });
            if (people != null) {
                peopleRepository.delete(people);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting people: " + e.getMessage(), e);
        }
    }

}
