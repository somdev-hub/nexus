package com.nexus.hr.payload;

import lombok.Data;

@Data
public class CommsPayload {

	private Long hrId;
	private Long payrollId;
	private Long recruitmentId;
	private Long applicantId;
	private Long applicantRecruitmentMappingId;
	private Long recruitmentInterviewId;
	
}
