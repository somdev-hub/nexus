package com.nexus.hr.payload;

import lombok.Data;

import java.util.List;

@Data
public class ApplicantApplication {
    private Long userId;
    private List<Long> hrDocumentIds;
    private Long recruitmentId;
}
