package com.nexus.core.entities;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "t_logs", schema = "core")
@AllArgsConstructor
@NoArgsConstructor
public class Logs {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String requestUrl;

	private String httpMethod;

	private int responseStatus;

	@Column(columnDefinition = "jsonb")
	private Object request;

	@Column(columnDefinition = "jsonb")
	private Object response;

	private Long userId;

	private Timestamp createdOn = new Timestamp(System.currentTimeMillis());

}
