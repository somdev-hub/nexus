package com.nexus.hr.payload.response;

import com.nexus.hr.model.entities.HrDocument;
import com.nexus.hr.model.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ApplicantTableResponse {
    private Long applicantId;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantCity;
    private String applicantState;
    private String applicantCountry;
    private Integer applicantAge;
    private Character applicantGender;
    private String previousCompany;
    private Double totalYearsOfExperience;
    private List<HrDocument> applicantDocuments;
    private ApplicationStatus applicationStatus;
}
