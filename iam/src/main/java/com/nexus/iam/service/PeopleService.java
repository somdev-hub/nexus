package com.nexus.iam.service;

import org.springframework.stereotype.Service;

@Service
public interface PeopleService {

    public void createPeople(Long userId, Long roleId);

    public void deletePeopleByUserId(Long userId);

}
