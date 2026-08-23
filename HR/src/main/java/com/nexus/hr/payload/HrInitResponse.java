package com.nexus.hr.payload;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HrInitResponse {
    private Long hrId;

    private String joiningLetterUrl;

    private String letterOfIntentUrl;

    private String compensationCardUrl;
}
