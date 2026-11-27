package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

import com.nexus.iam.dto.UserProfileDto;
import com.nexus.iam.dto.UserRegisterDto;
import com.nexus.iam.dto.UpdateUserProfileDto;
import com.nexus.iam.dto.ChangePasswordDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
	ResponseEntity<?> getUserById(Long userId);

	ResponseEntity<?> createUser(UserProfileDto userDto, MultipartFile[] files);

	ResponseEntity<?> updateUser(UserRegisterDto userDto, Long userId);

	ResponseEntity<?> updateUserProfile(UpdateUserProfileDto userDto, Long userId);

	ResponseEntity<?> changePassword(ChangePasswordDto passwordDto, Long userId);

	ResponseEntity<?> deleteUser(Long userId);

	ResponseEntity<?> getAllUsers(int page, int size);

	ResponseEntity<?> getAllEmployees(Long orgId, Integer page, Integer pageOffset);

	ResponseEntity<?> getChatContacts(Long orgId);

	ResponseEntity<?> updateProfilePhoto(MultipartFile file, Long userId);

	ResponseEntity<?> getUserDetails(Long userId);

	ResponseEntity<?> getUserByName(String name);

	ResponseEntity<?> searchUsersCrossOrg(String name, String orgType);

	ResponseEntity<?> validateUserOrganizationAccess(Long userId, Long organizationId);
}
