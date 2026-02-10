package com.nexus.iam.dto.response;

public record DeptOverview(String departmentName, Long organizationId, String departmentHead, Integer members, Integer roles) {
}
