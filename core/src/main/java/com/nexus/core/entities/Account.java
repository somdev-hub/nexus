package com.nexus.core.entities;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_accounts", schema = "core")
public class Account extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "account_id")
	private Long accountId;

	private Long org;

	private String name;

	private Double currentAccountBalance;

	@OneToMany(mappedBy = "primaryOrg")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Partnership> primaryPartnerships;

	@OneToMany(mappedBy = "secondaryOrg")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Partnership> secondaryPartnerships;

}
