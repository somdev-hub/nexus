package com.nexus.core.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

	/**
	 * Serialised JSON string. Must be a typed String with JdbcTypeCode JSON:
	 * Hibernate cannot bind an untyped java.lang.Object to a jsonb column
	 * (SQLState 07006 / "Unsupported Types value"), which used to make every
	 * log insert fail silently. Same pattern as EventOutbox.payload.
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String request;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String response;

	private Long userId;
}
