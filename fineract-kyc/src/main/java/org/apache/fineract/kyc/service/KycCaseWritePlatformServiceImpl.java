/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.kyc.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.kyc.api.KycApiConstants;
import org.apache.fineract.kyc.domain.KycCase;
import org.apache.fineract.kyc.domain.KycCaseFlag;
import org.apache.fineract.kyc.domain.KycCaseFlagRepository;
import org.apache.fineract.kyc.domain.KycCaseHistory;
import org.apache.fineract.kyc.domain.KycCaseHistoryRepository;
import org.apache.fineract.kyc.domain.KycCaseRepository;
import org.apache.fineract.kyc.domain.KycCaseRepositoryWrapper;
import org.apache.fineract.kyc.domain.KycCaseWorkflowStatus;
import org.apache.fineract.kyc.domain.KycDocument;
import org.apache.fineract.kyc.domain.KycDocumentRepository;
import org.apache.fineract.kyc.domain.KycDocumentStatus;
import org.apache.fineract.kyc.domain.KycFlagType;
import org.apache.fineract.kyc.domain.KycFlagTypeRepository;
import org.apache.fineract.kyc.domain.KycTier;
import org.apache.fineract.kyc.domain.KycTierDocumentRequirement;
import org.apache.fineract.kyc.domain.KycTierRepositoryWrapper;
import org.apache.fineract.kyc.serialization.KycCaseValidator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class KycCaseWritePlatformServiceImpl implements KycCaseWritePlatformService {

    private final KycCaseRepository repository;
    private final KycCaseRepositoryWrapper repositoryWrapper;
    private final KycCaseHistoryRepository historyRepository;
    private final KycTierRepositoryWrapper tierRepositoryWrapper;
    private final KycDocumentRepository documentRepository;
    private final KycCaseFlagRepository flagRepository;
    private final KycFlagTypeRepository flagTypeRepository;
    private final KycEvaluationService evaluationService;
    private final KycCaseValidator validator;
    private final KycAuditLogService auditLogService;
    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public CommandProcessingResult createCase(final JsonCommand command) {
        this.validator.validateForCreate(command.json());

        final Long clientId = command.longValueOfParameterNamed(KycApiConstants.clientIdParamName);

        KycTier tier = null;
        if (command.parameterExists(KycApiConstants.tierIdParamName)) {
            final Long tierId = command.longValueOfParameterNamed(KycApiConstants.tierIdParamName);
            tier = this.tierRepositoryWrapper.findOneWithNotFoundDetection(tierId);
        }

        final KycCase kycCase = KycCase.create(clientId, tier);
        this.repository.saveAndFlush(kycCase);

        final String comments = command.stringValueOfParameterNamed(KycApiConstants.commentsParamName);

        // Save initial history
        final Long actorId = context.authenticatedUser().getId();
        final KycCaseHistory history = KycCaseHistory.createWorkflowTransition(kycCase, null, KycCaseWorkflowStatus.EN_ATTENTE.getValue(),
                comments, actorId);
        this.historyRepository.save(history);

        // Audit log creation
        this.auditLogService.logWorkflowTransition(kycCase, null, KycCaseWorkflowStatus.EN_ATTENTE.getValue(), comments);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(clientId).withEntityId(kycCase.getId())
                .build();
    }

    @Override
    public CommandProcessingResult createCaseForClient(final Long clientId) {
        // Check if case already exists
        List<KycCase> existingCases = this.repositoryWrapper.findByClientId(clientId);
        if (!existingCases.isEmpty()) {
            KycCase latestCase = existingCases.stream().max(java.util.Comparator.comparing(KycCase::getId)).orElse(null);

            if (latestCase != null) {
                int status = latestCase.getWorkflowStatus();
                // Return existing case if it is NOT CONFORME (200), NOT FERME (600) and NOT REJETE (500)
                if (status != KycCaseWorkflowStatus.FERME.getValue() && status != KycCaseWorkflowStatus.REJETE.getValue()) {
                    return new CommandProcessingResultBuilder().withClientId(clientId).withEntityId(latestCase.getId()).build();
                }
            }
        }

        final KycCase kycCase = KycCase.create(clientId, null);
        // Force the initial status to EN_ATTENTE (100)
        kycCase.transitionWorkflow(KycCaseWorkflowStatus.EN_ATTENTE);
        this.repository.saveAndFlush(kycCase);

        final String comments = "Automatic case creation on client registration";

        // Save initial history
        final Long actorId = context.getAuthenticatedUserIfPresent() != null ? context.getAuthenticatedUserIfPresent().getId() : 1L; // fallback
        final KycCaseHistory history = KycCaseHistory.createWorkflowTransition(kycCase, null, KycCaseWorkflowStatus.EN_ATTENTE.getValue(),
                comments, actorId);
        this.historyRepository.save(history);

        // Audit log creation
        this.auditLogService.logWorkflowTransition(kycCase, null, KycCaseWorkflowStatus.EN_ATTENTE.getValue(), comments);

        return new CommandProcessingResultBuilder().withClientId(clientId).withEntityId(kycCase.getId()).build();
    }

    @Override
    public CommandProcessingResult transitionWorkflow(final Long caseId, final JsonCommand command) {
        this.validator.validateForWorkflowTransition(command.json());

        final KycCase kycCase = this.repositoryWrapper.findOneWithNotFoundDetection(caseId);

        final Integer newStatusValue = command.integerValueOfParameterNamed(KycApiConstants.workflowStatusParamName);
        final KycCaseWorkflowStatus newStatus = KycCaseWorkflowStatus.fromInt(newStatusValue);
        final String comments = command.stringValueOfParameterNamed(KycApiConstants.commentsParamName);

        final Map<String, Object> changes = kycCase.transitionWorkflow(newStatus);

        if (!changes.isEmpty()) {
            if (newStatus == KycCaseWorkflowStatus.CONFORME) {
                calculateAndAssignTier(kycCase);
            }

            this.repository.saveAndFlush(kycCase);

            // Save history
            final Long actorId = context.authenticatedUser().getId();
            final KycCaseHistory history = KycCaseHistory.createWorkflowTransition(kycCase, (Integer) changes.get("fromStatus"),
                    (Integer) changes.get("toStatus"), comments, actorId);
            this.historyRepository.save(history);

            // Audit log
            this.auditLogService.logWorkflowTransition(kycCase, (Integer) changes.get("fromStatus"), (Integer) changes.get("toStatus"),
                    comments);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(kycCase.getClientId())
                .withEntityId(caseId).with(changes).build();
    }

    private void calculateAndAssignTier(KycCase kycCase) {
        // Fetch all verified documents for the client
        List<KycDocument> verifiedDocs = this.documentRepository.findByClientIdAndVerificationStatus(kycCase.getClientId(),
                KycDocumentStatus.VERIFIED.getValue());

        Set<Long> verifiedDocTypeIds = verifiedDocs.stream().map(doc -> doc.getDocumentType().getId()).collect(Collectors.toSet());

        // Fetch all active tiers
        List<KycTier> allTiers = this.tierRepositoryWrapper.findAll();

        // Find the highest tier where the client meets all mandatory document requirements
        Optional<KycTier> highestEligibleTier = allTiers.stream().filter(KycTier::isActive).filter(tier -> {
            Set<Long> mandatoryDocTypeIds = tier.getDocumentRequirements().stream().filter(KycTierDocumentRequirement::isMandatory)
                    .map(req -> req.getDocumentType().getId()).collect(Collectors.toSet());
            return verifiedDocTypeIds.containsAll(mandatoryDocTypeIds);
        }).max(Comparator.comparing(KycTier::getLevel));

        if (highestEligibleTier.isPresent()) {
            KycTier tier = highestEligibleTier.get();
            // Update the KYC Case
            kycCase.updateTier(tier);

            // Update the Client table directly
            String sql = "UPDATE m_client SET kyc_tier_id = ? WHERE id = ?";
            this.jdbcTemplate.update(sql, tier.getId(), kycCase.getClientId());
        }
    }

    @Override
    public CommandProcessingResult assignAgent(final Long caseId, final JsonCommand command) {
        this.validator.validateForAssignment(command.json());

        final KycCase kycCase = this.repositoryWrapper.findOneWithNotFoundDetection(caseId);

        final Long assignedAgentId = command.longValueOfParameterNamed(KycApiConstants.assignedAgentIdParamName);
        final String comments = command.stringValueOfParameterNamed(KycApiConstants.commentsParamName);

        final Long oldAgentId = kycCase.getAssignedAgentId();

        final Map<String, Object> changes = kycCase.assign(assignedAgentId);

        if (!changes.isEmpty()) {
            this.repository.saveAndFlush(kycCase);

            // Save history for assignment change
            final Long actorId = context.authenticatedUser().getId();
            final KycCaseHistory history = KycCaseHistory.createAssignmentTransition(kycCase, (Integer) changes.get("fromAssignmentStatus"),
                    (Integer) changes.get("toAssignmentStatus"), comments, actorId);
            this.historyRepository.save(history);

            // Audit log for assignment change
            this.auditLogService.logAssignment(kycCase, oldAgentId, assignedAgentId, comments);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(kycCase.getClientId())
                .withEntityId(caseId).with(changes).build();
    }

    @Override
    public CommandProcessingResult updateRisk(final Long caseId, final JsonCommand command) {
        this.validator.validateForRiskUpdate(command.json());

        final KycCase kycCase = this.repositoryWrapper.findOneWithNotFoundDetection(caseId);

        final Integer riskLevel = command.integerValueOfParameterNamed(KycApiConstants.riskLevelParamName);
        final BigDecimal riskScore = command.bigDecimalValueOfParameterNamed(KycApiConstants.riskScoreParamName);
        final String riskReason = command.stringValueOfParameterNamed(KycApiConstants.riskReasonParamName);
        final String comments = command.stringValueOfParameterNamed(KycApiConstants.commentsParamName);

        final Map<String, Object> changes = kycCase.updateRisk(riskLevel, riskScore, riskReason);

        if (!changes.isEmpty()) {
            this.repository.saveAndFlush(kycCase);

            // Audit log for risk update
            this.auditLogService.logRiskUpdate(kycCase, changes, comments);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(kycCase.getClientId())
                .withEntityId(caseId).with(changes).build();
    }

    @Override
    public CommandProcessingResult updateEdd(final Long caseId, final JsonCommand command) {
        this.validator.validateForUpdateEdd(command.json());
        final KycCase kycCase = this.repositoryWrapper.findOneWithNotFoundDetection(caseId);

        boolean updated = false;

        if (command.parameterExists(KycApiConstants.escalationReasonParamName)) {
            kycCase.updateEscalationReason(command.stringValueOfParameterNamed(KycApiConstants.escalationReasonParamName));
            updated = true;
        }

        if (command.parameterExists(KycApiConstants.complianceNotesParamName)) {
            kycCase.updateComplianceNotes(command.stringValueOfParameterNamed(KycApiConstants.complianceNotesParamName));
            updated = true;
        }

        if (updated) {
            this.repository.saveAndFlush(kycCase);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(kycCase.getClientId())
                .withEntityId(caseId).build();
    }

    @Override
    public CommandProcessingResult addFlag(final Long caseId, final JsonCommand command) {
        this.validator.validateForAddFlag(command.json());
        final KycCase kycCase = this.repositoryWrapper.findOneWithNotFoundDetection(caseId);

        final String flagTypeCode = command.stringValueOfParameterNamed("flagTypeCode");
        final String remarks = command.stringValueOfParameterNamed("remarks");

        final KycFlagType flagType = this.flagTypeRepository.findByCode(flagTypeCode)
                .orElseThrow(() -> new org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException(
                        "error.msg.kyc.flag.type.not.found", "KYC Flag Type not found", flagTypeCode));

        final KycCaseFlag flag = KycCaseFlag.create(kycCase, flagType, remarks);
        this.flagRepository.saveAndFlush(flag);

        // Re-evaluate KYC status
        this.evaluationService.evaluateKycStatus(kycCase.getClientId());

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(kycCase.getClientId())
                .withEntityId(caseId).withSubEntityId(flag.getId()).build();
    }

    @Override
    public CommandProcessingResult resolveFlag(final Long caseId, final Long flagId, final JsonCommand command) {
        this.validator.validateForResolveFlag(command.json());
        final KycCase kycCase = this.repositoryWrapper.findOneWithNotFoundDetection(caseId);

        final KycCaseFlag flag = this.flagRepository.findById(flagId).orElseThrow(
                () -> new org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException("error.msg.kyc.flag.not.found",
                        "KYC Case Flag not found", flagId));

        if (!flag.getKycCase().getId().equals(caseId)) {
            throw new org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException("error.msg.kyc.flag.case.mismatch",
                    "KYC Case Flag does not belong to this case", flagId);
        }

        final Integer statusId = command.integerValueOfParameterNamed("statusId");
        final String resolutionRemarks = command.stringValueOfParameterNamed("resolutionRemarks");

        if (statusId == 200) {
            flag.resolve(resolutionRemarks);
        } else if (statusId == 300) {
            flag.markAsFalsePositive(resolutionRemarks);
        } else {
            throw new org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException("error.msg.kyc.flag.invalid.status",
                    "Invalid status for flag resolution", statusId);
        }

        this.flagRepository.saveAndFlush(flag);

        // Re-evaluate KYC status
        this.evaluationService.evaluateKycStatus(kycCase.getClientId());

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(kycCase.getClientId())
                .withEntityId(caseId).withSubEntityId(flag.getId()).build();
    }
}
