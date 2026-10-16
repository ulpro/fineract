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
package org.apache.fineract.crm.kyc.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.kyc.data.CrmKycReviewData;
import org.apache.fineract.crm.kyc.domain.CrmKycReview;
import org.apache.fineract.crm.kyc.domain.CrmKycReviewRepository;
import org.apache.fineract.crm.prospect.domain.CrmProspect;
import org.apache.fineract.crm.prospect.domain.CrmProspectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CrmKycReviewWritePlatformServiceImpl implements CrmKycReviewWritePlatformService {

    private final CrmKycReviewRepository reviewRepository;
    private final CrmProspectRepository prospectRepository;

    @Override
    public CrmKycReviewData createReview(CrmKycReviewData dto) {
        if (dto.getProspectId() == null && dto.getClientId() == null) {
            throw new IllegalArgumentException("Une revue KYC doit être liée à un prospect ou à un client.");
        }
        if (dto.getRiskLevelEnum() == null) {
            throw new IllegalArgumentException("Le niveau de risque LBC/FT est obligatoire.");
        }

        LocalDate reviewDate = dto.getReviewDate() != null ? dto.getReviewDate() : LocalDate.now();
        LocalDate nextReviewDate = CrmKycReview.computeNextReviewDate(reviewDate, dto.getRiskLevelEnum());

        CrmKycReview review = new CrmKycReview(
                dto.getProspectId(),
                dto.getClientId(),
                dto.getRiskLevelEnum(),
                reviewDate,
                nextReviewDate,
                dto.getReviewedByUserId(),
                dto.getReviewNotes(),
                dto.getTriggerEnum()
        );

        CrmKycReview saved = reviewRepository.save(review);

        // Mettre à jour le niveau de risque et les dates sur le prospect
        if (dto.getProspectId() != null) {
            updateProspectRiskLevel(dto.getProspectId(), dto.getRiskLevelEnum());
            prospectRepository.findById(dto.getProspectId()).ifPresent(p -> {
                p.setLastKycReviewDate(reviewDate);
                p.setNextKycReviewDate(nextReviewDate);
                prospectRepository.save(p);
            });
        }

        return mapToData(saved);
    }

    @Override
    public CrmKycReviewData validateReview(Long reviewId, Integer riskLevelEnum, String notes,
            Long validatedByUserId) {
        CrmKycReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Revue KYC non trouvée id: " + reviewId));

        review.setStatusEnum(1); // VALIDEE
        if (riskLevelEnum != null) {
            review.setRiskLevelEnum(riskLevelEnum);
            // Recalculer la prochaine date selon le nouveau niveau de risque
            review.setNextReviewDate(CrmKycReview.computeNextReviewDate(review.getReviewDate(), riskLevelEnum));
        }
        if (notes != null) review.setReviewNotes(notes);
        if (validatedByUserId != null) review.setReviewedByUserId(validatedByUserId);
        review.setLastModifiedDate(LocalDateTime.now());

        CrmKycReview saved = reviewRepository.save(review);

        // Propager le niveau de risque sur le prospect
        if (saved.getProspectId() != null && riskLevelEnum != null) {
            updateProspectRiskLevel(saved.getProspectId(), riskLevelEnum);
        }

        return mapToData(saved);
    }

    @Override
    public CrmKycReviewData rejectReview(Long reviewId, String notes, Long rejectedByUserId) {
        CrmKycReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Revue KYC non trouvée id: " + reviewId));

        review.setStatusEnum(2); // REJETEE
        if (notes != null) review.setReviewNotes(notes);
        if (rejectedByUserId != null) review.setReviewedByUserId(rejectedByUserId);
        review.setLastModifiedDate(LocalDateTime.now());

        return mapToData(reviewRepository.save(review));
    }

    @Override
    public void updateProspectRiskLevel(Long prospectId, Integer riskLevelEnum) {
        prospectRepository.findById(prospectId).ifPresent(p -> {
            p.setRiskLevelEnum(riskLevelEnum);
            prospectRepository.save(p);
        });
    }

    private CrmKycReviewData mapToData(CrmKycReview r) {
        boolean overdue = r.getNextReviewDate() != null && r.getNextReviewDate().isBefore(LocalDate.now())
                && Integer.valueOf(1).equals(r.getStatusEnum());
        return new CrmKycReviewData(
                r.getId(), r.getProspectId(), r.getClientId(),
                r.getRiskLevelEnum(), r.getStatusEnum(),
                r.getReviewDate(), r.getNextReviewDate(),
                r.getReviewedByUserId(), r.getReviewNotes(),
                r.getTriggerEnum(), r.getCreatedDate(), overdue);
    }
}
