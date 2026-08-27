package com.nexus.core.payload;

import com.nexus.core.entities.PartnershipStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnershipStatusTransitionDto {

	private PartnershipStatus newStatus;
	private String reason;
}