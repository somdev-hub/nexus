package com.nexus.hr.payload.response;

public record CompanyOpeningsCardDto(
        Long orgId,
        String orgName,
        long currentOpenings,
        long changeFromLastMonth
) {}
