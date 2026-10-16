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
import org.apache.fineract.crm.kyc.data.CrmKycDocumentData;
import org.apache.fineract.crm.kyc.domain.CrmKycDocument;
import org.apache.fineract.crm.kyc.domain.CrmKycDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmKycDocumentReadPlatformServiceImpl implements CrmKycDocumentReadPlatformService {

    private final CrmKycDocumentRepository kycRepository;

    @Override
    public List<CrmKycDocumentData> retrieveByProspectId(Long prospectId) {
        return kycRepository.findByProspectId(prospectId).stream()
                .map(this::mapToData)
                .collect(Collectors.toList());
    }

    @Override
    public List<CrmKycDocumentData> retrieveByClientId(Long clientId) {
        return kycRepository.findByClientId(clientId).stream()
                .map(this::mapToData)
                .collect(Collectors.toList());
    }

    @Override
    public CrmKycDocumentData retrieveById(Long documentId) {
        CrmKycDocument doc = kycRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document KYC non trouvé id: " + documentId));
        return mapToData(doc);
    }

    @Override
    public List<CrmKycDocumentData> retrieveExpiringWithinDays(int days) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(days);
        return kycRepository.findExpiringBetween(today, limit).stream()
                .map(doc -> {
                    CrmKycDocumentData data = mapToData(doc);
                    data.setExpiringWithin30Days(true);
                    return data;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CrmKycDocumentData> retrieveExpiredDocuments() {
        return kycRepository.findExpiredDocuments(LocalDate.now()).stream()
                .map(this::mapToData)
                .collect(Collectors.toList());
    }

    private CrmKycDocumentData mapToData(CrmKycDocument d) {
        boolean expiringWithin30Days = d.getExpiryDate() != null
                && !d.getExpiryDate().isBefore(LocalDate.now())
                && !d.getExpiryDate().isAfter(LocalDate.now().plusDays(30));

        return new CrmKycDocumentData(
                d.getId(),
                d.getProspectId(),
                d.getClientId(),
                d.getDocumentTypeEnum(),
                d.getDocumentTypeLabel(),
                d.getDocumentNumber(),
                d.getIssuingAuthority(),
                d.getIssuingCountry(),
                d.getIssueDate(),
                d.getExpiryDate(),
                d.getStatusEnum(),
                d.getFileReference(),
                d.getRiskLevelEnum(),
                d.getVerificationNotes(),
                d.getVerifiedByUserId(),
                d.getVerifiedOnDate(),
                d.getCreatedDate(),
                expiringWithin30Days
        );
    }
}
