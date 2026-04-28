package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.HrEntity;
import com.nexus.hr.model.entities.Recruitment;
import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.RecruitmentRepo;
import com.nexus.hr.service.interfaces.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@RequiredArgsConstructor
@Service
public class RecruitmentServiceImpl implements RecruitmentService {

    private final RecruitmentRepo recruitmentRepo;
    private final HrEntityRepo hrEntityRepo;

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
    public ResponseEntity<?> getAllRecruitments(Boolean isActive, Long empId, HiringType hiringType, HiringStatus hiringStatus, PageRequest pageRequest) {
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
                result = recruitmentRepo.findByAllFilters(isActive, empId, hiringType, hiringStatus, pageRequest);
            } else if (hasIsActive && hasEmpId && hasHiringType) {
                // isActive, empId, hiringType
                result = recruitmentRepo.findByIsActiveAndCreatedByEmployeeIdAndHiringType(isActive, empId, hiringType, pageRequest);
            } else if (hasIsActive && hasEmpId && hasHiringStatus) {
                // isActive, empId, hiringStatus
                result = recruitmentRepo.findByIsActiveAndCreatedByEmployeeIdAndHiringStatus(isActive, empId, hiringStatus, pageRequest);
            } else if (hasIsActive && hasHiringType && hasHiringStatus) {
                // isActive, hiringType, hiringStatus
                result = recruitmentRepo.findByIsActiveAndHiringTypeAndHiringStatus(isActive, hiringType, hiringStatus, pageRequest);
            } else if (hasEmpId && hasHiringType && hasHiringStatus) {
                // empId, hiringType, hiringStatus
                result = recruitmentRepo.findByCreatedByEmployeeIdAndHiringTypeAndHiringStatus(empId, hiringType, hiringStatus, pageRequest);
            } else if (hasIsActive && hasEmpId) {
                // isActive and empId
                result = recruitmentRepo.findByIsActiveAndCreatedByEmployeeId(isActive, empId, pageRequest);
            } else if (hasIsActive && hasHiringType) {
                // isActive and hiringType
                result = recruitmentRepo.findByIsActiveAndHiringType(isActive, hiringType, pageRequest);
            } else if (hasIsActive && hasHiringStatus) {
                // isActive and hiringStatus
                result = recruitmentRepo.findByIsActiveAndHiringStatus(isActive, hiringStatus, pageRequest);
            } else if (hasEmpId && hasHiringType) {
                // empId and hiringType
                result = recruitmentRepo.findByCreatedByEmployeeIdAndHiringType(empId, hiringType, pageRequest);
            } else if (hasEmpId && hasHiringStatus) {
                // empId and hiringStatus
                result = recruitmentRepo.findByCreatedByEmployeeIdAndHiringStatus(empId, hiringStatus, pageRequest);
            } else if (hasHiringType && hasHiringStatus) {
                // hiringType and hiringStatus
                result = recruitmentRepo.findByHiringTypeAndHiringStatus(hiringType, hiringStatus, pageRequest);
            } else if (hasIsActive) {
                // Only isActive
                result = recruitmentRepo.findByIsActive(isActive, pageRequest);
            } else if (hasEmpId) {
                // Only empId
                result = recruitmentRepo.findByCreatedByEmployeeId(empId, pageRequest);
            } else if (hasHiringType) {
                // Only hiringType
                result = recruitmentRepo.findByHiringType(hiringType, pageRequest);
            } else if (hasHiringStatus) {
                // Only hiringStatus
                result = recruitmentRepo.findByHiringStatus(hiringStatus, pageRequest);
            } else {
                // No filters provided - return all recruitments
                result = recruitmentRepo.findAll(pageRequest);
            }

            if (result.isEmpty()) {
                return ResponseEntity.ok(org.springframework.data.domain.Page.empty(pageRequest));
            }

            return ResponseEntity.ok(result);
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
}
