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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.kyc.domain.KycCase;
import org.apache.fineract.kyc.domain.KycCaseFlag;
import org.apache.fineract.kyc.domain.KycCaseFlagRepository;
import org.apache.fineract.kyc.domain.KycCaseRepository;
import org.apache.fineract.kyc.domain.KycCaseRepositoryWrapper;
import org.apache.fineract.kyc.domain.KycCaseWorkflowStatus;
import org.apache.fineract.kyc.domain.KycDocument;
import org.apache.fineract.kyc.domain.KycDocumentRepository;
import org.apache.fineract.kyc.domain.KycDocumentStatus;
import org.apache.fineract.kyc.domain.KycTier;
import org.apache.fineract.kyc.domain.KycTierDocumentRequirement;
import org.apache.fineract.kyc.domain.KycTierRepositoryWrapper;
import org.apache.fineract.kyc.domain.KycTierRequiredField;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycEvaluationServiceImpl implements KycEvaluationService {

    private final KycCaseRepository kycCaseRepository;
    private final KycCaseRepositoryWrapper kycCaseRepositoryWrapper;
    private final KycTierRepositoryWrapper kycTierRepositoryWrapper;
    private final KycCaseFlagRepository kycCaseFlagRepository;
    private final KycDocumentRepository documentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void evaluateKycStatus(final Long clientId) {
        log.info("Evaluating KYC Status for client ID: {}", clientId);

        // 1. Get the KYC Case for this client
        List<KycCase> cases = kycCaseRepositoryWrapper.findByClientId(clientId);
        if (cases.isEmpty()) {
            log.warn("No KYC Case found for client ID: {}", clientId);
            return;
        }

        KycCase kycCase = cases.stream().max(Comparator.comparing(KycCase::getId)).orElse(null);

        if (kycCase == null) {
            return;
        }

        // Don't auto-evaluate if the case is FERME or REJETE
        int status = kycCase.getWorkflowStatus();
        if (status == KycCaseWorkflowStatus.FERME.getValue() || status == KycCaseWorkflowStatus.REJETE.getValue()) {
            return;
        }

        // 2. Load active flags for this case
        List<KycCaseFlag> flags = this.kycCaseFlagRepository.findByKycCaseIdAndStatus(kycCase.getId(), 100); // 100 =
                                                                                                             // ACTIVE
        Integer maxOverrideStatus = null;

        for (KycCaseFlag flag : flags) {
            Integer override = flag.getFlagType().getOverrideStatus();
            if (override != null) {
                if (maxOverrideStatus == null || override > maxOverrideStatus) {
                    maxOverrideStatus = override;
                }
            }
        }

        // 3. Load Client details from DB
        Map<String, Object> clientData;
        try {
            clientData = this.jdbcTemplate.queryForMap("SELECT * FROM m_client WHERE id = ?", clientId);
        } catch (Exception e) {
            log.error("Failed to load client data for ID: {}", clientId, e);
            return;
        }

        Long clientTypeCvId = null;
        if (clientData.get("client_type_cv_id") != null) {
            clientTypeCvId = ((Number) clientData.get("client_type_cv_id")).longValue();
        }

        // 3. Load all Verified Documents
        List<KycDocument> verifiedDocs = this.documentRepository.findByClientIdAndVerificationStatus(clientId,
                KycDocumentStatus.VERIFIED.getValue());
        Set<Long> verifiedDocTypeIds = verifiedDocs.stream().map(doc -> doc.getDocumentType().getId()).collect(Collectors.toSet());

        // 4. Find the best matching Tier
        List<KycTier> allTiers = this.kycTierRepositoryWrapper.findAll();

        final Long finalClientTypeCvId = clientTypeCvId;
        KycTier bestTier = null;

        for (KycTier tier : allTiers) {
            if (!tier.isActive()) {
                continue;
            }

            // Match client type if tier has a specific one
            if (tier.getClientTypeCvId() != null && finalClientTypeCvId != null && !tier.getClientTypeCvId().equals(finalClientTypeCvId)) {
                continue;
            }

            boolean meetsDocs = meetsDocumentRequirements(tier, verifiedDocTypeIds);
            boolean meetsFields = meetsFieldRequirements(tier, clientData);

            if (meetsDocs && meetsFields) {
                if (bestTier == null || tier.getLevel() > bestTier.getLevel()) {
                    bestTier = tier;
                }
            }
        }

        // 5. Update KYC Case based on Flags or Tier
        boolean changed = false;

        // Apply Flag Override first
        if (maxOverrideStatus != null) {
            KycCaseWorkflowStatus targetStatus = KycCaseWorkflowStatus.fromInt(maxOverrideStatus);
            if (kycCase.getWorkflowStatusEnum() != targetStatus) {
                log.info("Client {} KYC Case forced to {} due to active flags", clientId, targetStatus.getValue());
                kycCase.transitionWorkflow(targetStatus);
                changed = true;
            }
            if (bestTier != null && (kycCase.getTier() == null || !kycCase.getTier().getId().equals(bestTier.getId()))) {
                kycCase.updateTier(bestTier);
                this.jdbcTemplate.update("UPDATE m_client SET kyc_tier_id = ? WHERE id = ?", bestTier.getId(), clientId);
                changed = true;
            }
        } else {
            // Normal Tier Evaluation
            if (bestTier != null) {
                if (kycCase.getTier() == null || !kycCase.getTier().getId().equals(bestTier.getId())) {
                    kycCase.updateTier(bestTier);
                    log.info("Client {} promoted to KYC Tier: {}", clientId, bestTier.getName());
                    this.jdbcTemplate.update("UPDATE m_client SET kyc_tier_id = ? WHERE id = ?", bestTier.getId(), clientId);
                    changed = true;
                }

                if (kycCase.getWorkflowStatusEnum() != KycCaseWorkflowStatus.CONFORME) {
                    kycCase.transitionWorkflow(KycCaseWorkflowStatus.CONFORME);
                    changed = true;
                }

                if (kycCase.getWorkflowStatusEnum() == KycCaseWorkflowStatus.CONFORME && bestTier.getValidityDays() != null) {
                    java.time.LocalDate nextReview = org.apache.fineract.infrastructure.core.service.DateUtils.getBusinessLocalDate()
                            .plusDays(bestTier.getValidityDays());
                    kycCase.updateNextReviewDate(nextReview);
                    changed = true;
                }
            } else {
                if (kycCase.getWorkflowStatusEnum() == KycCaseWorkflowStatus.CONFORME) {
                    kycCase.transitionWorkflow(KycCaseWorkflowStatus.EN_REVISION);
                    changed = true;
                }
            }
        }

        if (changed) {
            this.kycCaseRepository.saveAndFlush(kycCase);
        }
    }

    private boolean meetsDocumentRequirements(KycTier tier, Set<Long> verifiedDocTypeIds) {
        Set<Long> mandatoryDocTypeIds = tier.getDocumentRequirements().stream().filter(KycTierDocumentRequirement::isMandatory)
                .map(req -> req.getDocumentType().getId()).collect(Collectors.toSet());
        return verifiedDocTypeIds.containsAll(mandatoryDocTypeIds);
    }

    private boolean meetsFieldRequirements(KycTier tier, Map<String, Object> clientData) {
        for (KycTierRequiredField reqField : tier.getRequiredFields()) {
            String fieldName = reqField.getFieldName();
            Object value = clientData.get(fieldName);
            if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
                return false;
            }
        }
        return true;
    }
}
