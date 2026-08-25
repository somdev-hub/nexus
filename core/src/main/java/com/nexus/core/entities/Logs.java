package com.nexus.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_logs", schema = "core")
public class Logs extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "log_id")
	private Long logId;

	private String requestUrl;

	private String httpMethod;

	private int responseStatus;

	@Column(columnDefinition = "jsonb")
	private Object request;

	@Column(columnDefinition = "jsonb")
	private Object response;

	private Long userId;
}
