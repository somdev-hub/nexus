package com.nexus.core.payload;

import com.nexus.core.entities.PartnershipStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnershipStatusTransitionDto {

	@NotNull(message = "New status is required")
	private PartnershipStatus newStatus;

	private String reason;
}