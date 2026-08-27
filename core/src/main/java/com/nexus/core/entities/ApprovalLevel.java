package com.nexus.core.entities;

public enum ApprovalLevel {
	AUTO, // Below $10,000 - automatic approval
	MANAGER, // $10,000 - $50,000 - manager approval
	DIRECTOR // Above $50,000 - director approval
}