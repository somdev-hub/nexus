package com.nexus.iam.dto.response;

public record DeptOverview(Long departmentId, String departmentName, Long organizationId, String departmentHead,
                           Integer members, Integer roles) {
}
