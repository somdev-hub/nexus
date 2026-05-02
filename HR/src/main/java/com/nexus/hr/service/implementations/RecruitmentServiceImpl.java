package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.HrEntity;
import com.nexus.hr.model.entities.Recruitment;
import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import com.nexus.hr.payload.response.RecruitmentTableResponse;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.RecruitmentRepo;
import com.nexus.hr.service.interfaces.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RecruitmentServiceImpl implements RecruitmentService {

    private final RecruitmentRepo recruitmentRepo;
    private final HrEntityRepo hrEntityRepo;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<?> createRecruitment(Recruitment recruitment, Long empId) {
        if (ObjectUtils.isEmpty(recruitment) || ObjectUtils.isEmpty(empId)) {
            throw new ServiceLevelException(
                    "RecruimentService",
                    "Required recruitment body and empId missing",
                    "createRecruitment",
                    "Missing required data exception",
                    "Required data recruitment and empId are missing"
            );
        }
        try {
            HrEntity hrEntity = hrEntityRepo.findByEmployeeId(empId).orElseThrow(() -> new ResourceNotFoundException(
                    "HrEntity",
                    "employeeId",
                    empId.toString()
            ));
            recruitment.setCreatedBy(hrEntity);
            recruitment.setHiringStatus(HiringStatus.OPEN);
            Recruitment savedRecruitment = recruitmentRepo.save(recruitment);
            return ResponseEntity.ok(savedRecruitment);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruimentService",
                    "Error occurred while creating recruitment",
                    "createRecruitment",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getRecruitment(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new ServiceLevelException(
                    "RecruimentService",
                    "Recruitment id is missing",
                    "getRecruitment",
                    "Missing required data exception",
                    "Required data recruitment id is missing"
            );
        }
        try {
            Recruitment recruitment = recruitmentRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                    "Recruitment",
                    "id",
                    id.toString()
            ));
            return ResponseEntity.ok(recruitment);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruimentService",
                    "Error occurred while fetching recruitment",
                    "getRecruitment",
                    "Service level exception",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getAllRecruitments(Long orgId, Boolean isActive, Long empId, HiringType hiringType, HiringStatus hiringStatus, Pageable pageRequest) {
        try {
            // Validate pageRequest
            if (ObjectUtils.isEmpty(pageRequest)) {
                throw new ServiceLevelException(
                        "RecruimentService",
                        "PageRequest is required",
                        "getAllRecruitments",
                        "Missing required data exception",
                        "PageRequest cannot be null or empty"
                );
            }

            // Count how many filters are provided
            boolean hasIsActive = !ObjectUtils.isEmpty(isActive);
            boolean hasEmpId = !ObjectUtils.isEmpty(empId);
            boolean hasHiringType = !ObjectUtils.isEmpty(hiringType);
            boolean hasHiringStatus = !ObjectUtils.isEmpty(hiringStatus);

            // Call appropriate repository method based on filter combination
            org.springframework.data.domain.Page<Recruitment> result;

            if (hasIsActive && hasEmpId && hasHiringType && hasHiringStatus) {
                // All four filters provided
                result = recruitmentRepo.findByAllFilters(orgId, isActive, empId, hiringType, hiringStatus, pageRequest);
            } else if (hasIsActive && hasEmpId && hasHiringType) {
                // isActive, empId, hiringType
                result = recruitmentRepo.findByOrgIdAndIsActiveAndCreatedByEmployeeIdAndHiringType(orgId, isActive, empId, hiringType, pageRequest);
            } else if (hasIsActive && hasEmpId && hasHiringStatus) {
                // isActive, empId, hiringStatus
                result = recruitmentRepo.findByOrgIdAndIsActiveAndCreatedByEmployeeIdAndHiringStatus(orgId, isActive, empId, hiringStatus, pageRequest);
            } else if (hasIsActive && hasHiringType && hasHiringStatus) {
                // isActive, hiringType, hiringStatus
                result = recruitmentRepo.findByOrgIdAndIsActiveAndHiringTypeAndHiringStatus(orgId, isActive, hiringType, hiringStatus, pageRequest);
            } else if (hasEmpId && hasHiringType && hasHiringStatus) {
                // empId, hiringType, hiringStatus
                result = recruitmentRepo.findByOrgIdAndCreatedByEmployeeIdAndHiringTypeAndHiringStatus(orgId, empId, hiringType, hiringStatus, pageRequest);
            } else if (hasIsActive && hasEmpId) {
                // isActive and empId
                result = recruitmentRepo.findByOrgIdAndIsActiveAndCreatedByEmployeeId(orgId, isActive, empId, pageRequest);
            } else if (hasIsActive && hasHiringType) {
                // isActive and hiringType
                result = recruitmentRepo.findByOrgIdAndIsActiveAndHiringType(orgId, isActive, hiringType, pageRequest);
            } else if (hasIsActive && hasHiringStatus) {
                // isActive and hiringStatus
                result = recruitmentRepo.findByOrgIdAndIsActiveAndHiringStatus(orgId, isActive, hiringStatus, pageRequest);
            } else if (hasEmpId && hasHiringType) {
                // empId and hiringType
                result = recruitmentRepo.findByOrgIdAndCreatedByEmployeeIdAndHiringType(orgId, empId, hiringType, pageRequest);
            } else if (hasEmpId && hasHiringStatus) {
                // empId and hiringStatus
                result = recruitmentRepo.findByOrgIdAndCreatedByEmployeeIdAndHiringStatus(orgId, empId, hiringStatus, pageRequest);
            } else if (hasHiringType && hasHiringStatus) {
                // hiringType and hiringStatus
                result = recruitmentRepo.findByOrgIdAndHiringTypeAndHiringStatus(orgId, hiringType, hiringStatus, pageRequest);
            } else if (hasIsActive) {
                // Only isActive
                result = recruitmentRepo.findByOrgIdAndIsActive(orgId, isActive, pageRequest);
            } else if (hasEmpId) {
                // Only empId
                result = recruitmentRepo.findByOrgIdAndCreatedByEmployeeId(orgId, empId, pageRequest);
            } else if (hasHiringType) {
                // Only hiringType
                result = recruitmentRepo.findByOrgIdAndHiringType(orgId, hiringType, pageRequest);
            } else if (hasHiringStatus) {
                // Only hiringStatus
                result = recruitmentRepo.findByOrgIdAndHiringStatus(orgId, hiringStatus, pageRequest);
            } else {
                // No filters provided - return all recruitments for the organization
                result = recruitmentRepo.findByOrgId(orgId, pageRequest);
            }

            Page<RecruitmentTableResponse> mappedResults = result.map(recruitment -> {
                RecruitmentTableResponse map = modelMapper.map(recruitment, RecruitmentTableResponse.class);
                map.setHiringManager(recruitment.getCreatedBy().getEmployeeId());
                return map;
            });

            if (result.isEmpty()) {
                return ResponseEntity.ok(Page.empty(pageRequest));
            }

            return ResponseEntity.ok(mappedResults);
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruimentService",
                    "Error occurred while fetching recruitments",
                    "getAllRecruitments",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> updateRecruitment(Recruitment recruitment, Long empId) {
        if (ObjectUtils.isEmpty(recruitment) || ObjectUtils.isEmpty(empId)) {
            throw new ServiceLevelException(
                    "RecruimentService",
                    "Required recruitment body and empId missing",
                    "updateRecruitment",
                    "Missing required data exception",
                    "Required data recruitment and empId are missing"
            );
        }
        try {
            Recruitment existingRecruitment = recruitmentRepo.findById(recruitment.getRecruitmentId()).orElseThrow(() -> new ResourceNotFoundException(
                    "Recruitment",
                    "id",
                    recruitment.getRecruitmentId().toString()
            ));
            if (!existingRecruitment.getCreatedBy().getEmployeeId().equals(empId)) {
                throw new ServiceLevelException(
                        "RecruimentService",
                        "Unauthorized update attempt",
                        "updateRecruitment",
                        "Unauthorized access exception",
                        "Employee with id " + empId + " is not authorized to update this recruitment"
                );
            }
            if (recruitment.getTitle() != null) {
                existingRecruitment.setTitle(recruitment.getTitle());
            }
            if (recruitment.getShortDescription() != null) {
                existingRecruitment.setShortDescription(recruitment.getShortDescription());
            }
            if (recruitment.getDescription() != null) {
                existingRecruitment.setDescription(recruitment.getDescription());
            }
            if (recruitment.getOrgId() != null) {
                existingRecruitment.setOrgId(recruitment.getOrgId());
            }
            if (recruitment.getDepartmentName() != null) {
                existingRecruitment.setDepartmentName(recruitment.getDepartmentName());
            }
            if (recruitment.getDepartmentId() != null) {
                existingRecruitment.setDepartmentId(recruitment.getDepartmentId());
            }
            if (recruitment.getRoleName() != null) {
                existingRecruitment.setRoleName(recruitment.getRoleName());
            }
            if (recruitment.getOpeningTillDate() != null) {
                existingRecruitment.setOpeningTillDate(recruitment.getOpeningTillDate());
            }
            if (recruitment.getTotalCompensation() != null) {
                existingRecruitment.setTotalCompensation(recruitment.getTotalCompensation());
            }
            if (recruitment.getHiringType() != null) {
                existingRecruitment.setHiringType(recruitment.getHiringType());
            }
            if (recruitment.getHiringStatus() != null) {
                existingRecruitment.setHiringStatus(recruitment.getHiringStatus());
            }

            Recruitment updatedRecruitment = recruitmentRepo.save(existingRecruitment);
            return ResponseEntity.ok(updatedRecruitment);
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruimentService",
                    "Error occurred while updating recruitment",
                    "updateRecruitment",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getClosedRecruitments(Long orgId, Pageable of) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "RecruimentService",
                    "Organization id is missing",
                    "getClosedRecruitments",
                    "Missing required data exception",
                    "Required data organization id is missing"
            );
        }
        try {
            Page<Recruitment> closedRecruitments = recruitmentRepo.findByOrgIdAndHiringStatusIn(orgId, List.of(HiringStatus.CLOSED, HiringStatus.HIRED), of);
            Page<RecruitmentTableResponse> mappedResults = closedRecruitments.map(recruitment -> {
                RecruitmentTableResponse map = modelMapper.map(recruitment, RecruitmentTableResponse.class);
                map.setHiringManager(recruitment.getCreatedBy().getEmployeeId());
                return map;
            });
            return ResponseEntity.ok(mappedResults);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruimentService",
                    "Error occurred while fetching closed recruitments",
                    "getClosedRecruitments",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }
}
