package com.nexus.iam.dto;

import com.nexus.iam.entities.PermissionAction;
import com.nexus.iam.entities.ResourceType;
import lombok.Data;

import java.util.Set;

@Data
public class GrantPermissionDto {

    private String resourceName;
    private String description;
    private ResourceType resourceType;
    private String role;
    private Set<PermissionAction> actions;
    private Long departmentId;
    private String resourceUrl;
    private String featureId;

}
