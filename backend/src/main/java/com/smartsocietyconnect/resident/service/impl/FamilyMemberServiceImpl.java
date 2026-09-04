package com.smartsocietyconnect.resident.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartsocietyconnect.resident.dto.request.AddFamilyMemberRequest;
import com.smartsocietyconnect.resident.dto.request.UpdateFamilyMemberRequest;
import com.smartsocietyconnect.resident.dto.response.FamilyMemberResponse;
import com.smartsocietyconnect.resident.entity.FamilyMember;
import com.smartsocietyconnect.resident.entity.RelationType;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.repository.FamilyMemberRepository;
import com.smartsocietyconnect.resident.repository.ResidentRepository;
import com.smartsocietyconnect.resident.service.FamilyMemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link FamilyMemberService}.
 *
 * <p>Handles adding, fetching, counting, updating, and deleting family members.
 *
 * <p>Important database alignment:
 * {@code family_members.age} is {@code TINYINT UNSIGNED}. The entity uses
 * {@code Byte}, and request DTOs usually use {@code Integer}, so the service
 * converts using {@code byteValue()}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FamilyMemberServiceImpl implements FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;
    private final ResidentRepository residentRepository;

    // ==========================================================================
    // CREATE
    // ==========================================================================

    @Override
    public FamilyMemberResponse addFamilyMember(AddFamilyMemberRequest request) {
        log.info("Adding family member for resident id: {}", request.getResidentId());

        Resident resident = residentRepository.findById(request.getResidentId())
                .orElseThrow(() -> new ResidentException(
                        "Resident not found with id: " + request.getResidentId()));

        validateUniqueRelation(resident.getResidentId(), request.getRelation());

        if (request.getMobile() != null
                && familyMemberRepository.existsByMobile(request.getMobile())) {
            throw new ResidentException("Mobile number already registered: " + request.getMobile());
        }

        FamilyMember member = FamilyMember.builder()
                .resident(resident)
                .memberName(request.getMemberName())
                .relation(request.getRelation())
                .age(request.getAge() != null ? request.getAge().byteValue() : null)
                .mobile(request.getMobile())
                .build();

        return mapToResponse(familyMemberRepository.save(member));
    }

    // ==========================================================================
    // READ
    // ==========================================================================

    @Override
    @Transactional(readOnly = true)
    public FamilyMemberResponse getFamilyMemberById(Integer memberId) {
        return mapToResponse(findMemberById(memberId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> getFamilyMembersByResident(Integer residentId) {
        if (!residentRepository.existsById(residentId)) {
            throw new ResidentException("Resident not found with id: " + residentId);
        }

        return familyMemberRepository.findByResidentResidentId(residentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFamilyMembersByResident(Integer residentId) {
        if (!residentRepository.existsById(residentId)) {
            throw new ResidentException("Resident not found with id: " + residentId);
        }

        return familyMemberRepository.countByResidentResidentId(residentId);
    }

    // ==========================================================================
    // UPDATE
    // ==========================================================================

    @Override
    public FamilyMemberResponse updateFamilyMember(
            Integer memberId,
            UpdateFamilyMemberRequest request
    ) {
        FamilyMember member = findMemberById(memberId);

        if (request.getMobile() != null
                && !request.getMobile().equals(member.getMobile())
                && familyMemberRepository.existsByMobile(request.getMobile())) {
            throw new ResidentException("Mobile number already registered: " + request.getMobile());
        }

        if (request.getMemberName() != null) {
            member.setMemberName(request.getMemberName());
        }
        if (request.getAge() != null) {
            member.setAge(request.getAge().byteValue());
        }
        if (request.getMobile() != null) {
            member.setMobile(request.getMobile());
        }

        return mapToResponse(familyMemberRepository.save(member));
    }

    // ==========================================================================
    // DELETE
    // ==========================================================================

    @Override
    public void deleteFamilyMember(Integer memberId) {
        FamilyMember member = findMemberById(memberId);
        familyMemberRepository.delete(member);
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    private FamilyMember findMemberById(Integer memberId) {
        return familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResidentException(
                        "Family member not found with id: " + memberId));
    }

    private void validateUniqueRelation(Integer residentId, RelationType relation) {
        if (relation == RelationType.SPOUSE
                || relation == RelationType.FATHER
                || relation == RelationType.MOTHER) {

            boolean exists = familyMemberRepository
                    .existsByResidentResidentIdAndRelation(residentId, relation);

            if (exists) {
                throw new ResidentException(
                        relation + " already exists for resident id: " + residentId);
            }
        }
    }

    private FamilyMemberResponse mapToResponse(FamilyMember member) {
        return FamilyMemberResponse.builder()
                .memberId(member.getMemberId())
                .residentId(member.getResident().getResidentId())
                .memberName(member.getMemberName())
                .relation(member.getRelation())
                .age(member.getAge())
                .mobile(member.getMobile())
                .createdAt(member.getCreatedAt())
                .build();
    }
}