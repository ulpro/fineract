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
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.kyc.data.CrmKycReviewData;
import org.apache.fineract.crm.kyc.domain.CrmKycReview;
import org.apache.fineract.crm.kyc.domain.CrmKycReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmKycReviewReadPlatformServiceImpl implements CrmKycReviewReadPlatformService {

    private final CrmKycReviewRepository reviewRepository;

    @Override
    public List<CrmKycReviewData> retrieveByProspectId(Long prospectId) {
        return reviewRepository.findByProspectIdOrderByReviewDateDesc(prospectId).stream()
                .map(this::mapToData).collect(Collectors.toList());
    }

    @Override
    public List<CrmKycReviewData> retrieveByClientId(Long clientId) {
        return reviewRepository.findByClientIdOrderByReviewDateDesc(clientId).stream()
                .map(this::mapToData).collect(Collectors.toList());
    }

    @Override
    public CrmKycReviewData retrieveById(Long reviewId) {
        CrmKycReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Revue KYC non trouvée id: " + reviewId));
        return mapToData(review);
    }

    @Override
    public List<CrmKycReviewData> retrieveOverdueReviews() {
        return reviewRepository.findOverdueReviews(LocalDate.now()).stream()
                .map(r -> { CrmKycReviewData d = mapToData(r); d.setOverdue(true); return d; })
                .collect(Collectors.toList());
    }

    @Override
    public List<CrmKycReviewData> retrieveReviewsDueWithinDays(int days) {
        LocalDate today = LocalDate.now();
        return reviewRepository.findReviewsDueBetween(today, today.plusDays(days)).stream()
                .map(this::mapToData).collect(Collectors.toList());
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
