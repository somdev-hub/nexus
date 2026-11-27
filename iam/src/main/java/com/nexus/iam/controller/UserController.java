package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.UserProfileDto;
import com.nexus.iam.dto.UpdateUserProfileDto;
import com.nexus.iam.dto.ChangePasswordDto;
import com.nexus.iam.security.CustomUserPrincipal;
import com.nexus.iam.service.UserService;
import com.nexus.iam.utils.KeycloakTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/iam/users")
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private KeycloakTokenUtil keycloakTokenUtil;

	@LogActivity("Update User Profile")
	@PutMapping("/update")
	public ResponseEntity<?> updateUserProfile(@RequestParam("userId") Long userId,
			@RequestBody UpdateUserProfileDto user, Authentication authentication, HttpServletRequest request) {
		if (ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(user)) {
			return new ResponseEntity<>("User ID and profile details must not be null", HttpStatus.BAD_REQUEST);
		}

		Long authenticatedUserId = getAuthenticatedUserId(authentication, request);
		if (authenticatedUserId == null || !authenticatedUserId.equals(userId)) {
			return new ResponseEntity<>("You can only update your own profile", HttpStatus.FORBIDDEN);
		}

		return userService.updateUserProfile(user, userId);
	}

	@LogActivity("Add User")
	@PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> addUser(@RequestPart(value = "files", required = false) MultipartFile[] files,
			@RequestPart(value = "dto", required = true) UserProfileDto user) {

		if (ObjectUtils.isEmpty(user)) {
			return new ResponseEntity<>("Request body must not be null", HttpStatus.BAD_REQUEST);
		}

		return userService.createUser(user, files);
	}

	@LogActivity("Get All Employees")
	@GetMapping("/employees")
	public ResponseEntity<?> getAllEmployees(@RequestParam(value = "orgId", required = true) Long orgId,
			@RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
			@RequestParam(value = "pageOffset", defaultValue = "10", required = false) Integer pageOffset) {
		if (ObjectUtils.isEmpty(orgId)) {
			return new ResponseEntity<>("Org id must not be null", HttpStatus.BAD_REQUEST);
		}

		return userService.getAllEmployees(orgId, page, pageOffset);
	}

	@GetMapping("/chat-contacts")
	public ResponseEntity<?> getChatContacts(@RequestParam(value = "orgId", required = true) Long orgId) {
		if (ObjectUtils.isEmpty(orgId)) {
			return new ResponseEntity<>("Org id must not be null", HttpStatus.BAD_REQUEST);
		}
		return userService.getChatContacts(orgId);
	}

	@LogActivity("Update Profile Photo")
	@PostMapping(value = "/update/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> updateProfilePhoto(@RequestParam("file") MultipartFile file,
			@RequestParam("userId") Long userId, Authentication authentication, HttpServletRequest request) {
		if (ObjectUtils.isEmpty(file) || ObjectUtils.isEmpty(userId)) {
			return new ResponseEntity<>("File and User ID must not be null", HttpStatus.BAD_REQUEST);
		}

		Long authenticatedUserId = getAuthenticatedUserId(authentication, request);
		if (authenticatedUserId == null || !authenticatedUserId.equals(userId)) {
			return new ResponseEntity<>("You can only update your own profile photo", HttpStatus.FORBIDDEN);
		}

		return userService.updateProfilePhoto(file, userId);
	}

	@LogActivity("Change User Password")
	@PostMapping("/update/password")
	public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDto password,
			Authentication authentication, HttpServletRequest request) {
		Long authenticatedUserId = getAuthenticatedUserId(authentication, request);
		if (authenticatedUserId == null) {
			return new ResponseEntity<>("Unable to identify the authenticated user", HttpStatus.FORBIDDEN);
		}

		return userService.changePassword(password, authenticatedUserId);
	}

	@LogActivity("Get User Details")
	@GetMapping(value = "/get-user")
	public ResponseEntity<?> getUserDetails(@RequestParam("userId") Long userId) {
		if (ObjectUtils.isEmpty(userId)) {
			return new ResponseEntity<>("User ID must not be null", HttpStatus.BAD_REQUEST);
		}

		return userService.getUserDetails(userId);
	}

	@DeleteMapping("/delete")
	@LogActivity("Delete User")
	public ResponseEntity<?> deleteUser(@RequestParam("userId") Long userId) {
		if (ObjectUtils.isEmpty(userId)) {
			return new ResponseEntity<>("User ID must not be null", HttpStatus.BAD_REQUEST);
		}
		return userService.deleteUser(userId);
	}

	@GetMapping("/get-user-by-name")
	public ResponseEntity<?> getUserByName(@RequestParam("name") String name) {
		if (ObjectUtils.isEmpty(name)) {
			return new ResponseEntity<>("Name must not be null", HttpStatus.BAD_REQUEST);
		}
		return userService.getUserByName(name);
	}

	/**
	 * Search users across organizations for inter-org chat.
	 * GET /iam/users/search-cross-org?name=...&orgType=...
	 */
	@GetMapping("/search-cross-org")
	public ResponseEntity<?> searchUsersCrossOrg(
			@RequestParam("name") String name,
			@RequestParam(required = false) String orgType) {
		if (ObjectUtils.isEmpty(name)) {
			return new ResponseEntity<>("Name must not be null", HttpStatus.BAD_REQUEST);
		}
		return userService.searchUsersCrossOrg(name, orgType);
	}

	@LogActivity("Validate User Organization Access")
	@GetMapping("/{userId}/organizations/{organizationId}/access")
	public ResponseEntity<?> validateUserOrganizationAccess(@PathVariable Long userId,
			@PathVariable Long organizationId) {
		if (ObjectUtils.isEmpty(userId)) {
			return new ResponseEntity<>("User ID must not be null", HttpStatus.BAD_REQUEST);
		}
		if (ObjectUtils.isEmpty(organizationId)) {
			return new ResponseEntity<>("Organization ID must not be null", HttpStatus.BAD_REQUEST);
		}
		return userService.validateUserOrganizationAccess(userId, organizationId);
	}

	private Long getAuthenticatedUserId(Authentication authentication, HttpServletRequest request) {
		if (authentication == null) {
			return null;
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof CustomUserPrincipal userPrincipal) {
			return userPrincipal.getId();
		}

		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		return ObjectUtils.isEmpty(authorization) ? null
				: keycloakTokenUtil.extractUserIdFromDatabase(authorization);
	}
}
