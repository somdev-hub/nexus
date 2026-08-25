package com.nexus.core.entities;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@MappedSuperclass
public abstract class BaseEntity {

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Timestamp updatedAt;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;

	@PrePersist
	protected void onCreate() {
		if (isActive == null) {
			isActive = true;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		// updatedAt is automatically handled by @UpdateTimestamp
	}
}