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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.kyc.data.CrmKycDocumentData;
import org.apache.fineract.crm.kyc.domain.CrmKycDocument;
import org.apache.fineract.crm.kyc.domain.CrmKycDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CrmKycDocumentWritePlatformServiceImpl implements CrmKycDocumentWritePlatformService {

    private final CrmKycDocumentRepository kycRepository;

    @Override
    public CrmKycDocumentData createDocument(CrmKycDocumentData dto) {
        if (dto.getDocumentNumber() == null || dto.getDocumentNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro de pièce KYC est obligatoire.");
        }
        if (dto.getDocumentTypeEnum() == null) {
            throw new IllegalArgumentException("Le type de pièce KYC est obligatoire.");
        }
        if (dto.getProspectId() == null && dto.getClientId() == null) {
            throw new IllegalArgumentException("Une pièce KYC doit être liée à un prospect ou à un client.");
        }

        CrmKycDocument doc = new CrmKycDocument(
                dto.getProspectId(),
                dto.getClientId(),
                dto.getDocumentTypeEnum(),
                dto.getDocumentTypeLabel(),
                dto.getDocumentNumber().trim(),
                dto.getIssuingAuthority(),
                dto.getIssuingCountry(),
                dto.getIssueDate(),
                dto.getExpiryDate(),
                dto.getFileReference()
        );

        if (dto.getRiskLevelEnum() != null) {
            doc.setRiskLevelEnum(dto.getRiskLevelEnum());
        }
        if (dto.getStatusEnum() != null) {
            doc.setStatusEnum(dto.getStatusEnum());
        }

        CrmKycDocument saved = kycRepository.save(doc);
        return mapToData(saved);
    }

    @Override
    public CrmKycDocumentData updateDocument(Long documentId, CrmKycDocumentData dto) {
        CrmKycDocument doc = kycRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document KYC non trouvé id: " + documentId));

        if (dto.getDocumentNumber() != null && !dto.getDocumentNumber().trim().isEmpty()) {
            doc.setDocumentNumber(dto.getDocumentNumber().trim());
        }
        if (dto.getDocumentTypeEnum() != null) doc.setDocumentTypeEnum(dto.getDocumentTypeEnum());
        if (dto.getDocumentTypeLabel() != null) doc.setDocumentTypeLabel(dto.getDocumentTypeLabel());
        if (dto.getIssuingAuthority() != null) doc.setIssuingAuthority(dto.getIssuingAuthority());
        if (dto.getIssuingCountry() != null) doc.setIssuingCountry(dto.getIssuingCountry());
        if (dto.getIssueDate() != null) doc.setIssueDate(dto.getIssueDate());
        if (dto.getExpiryDate() != null) doc.setExpiryDate(dto.getExpiryDate());
        if (dto.getFileReference() != null) doc.setFileReference(dto.getFileReference());
        if (dto.getRiskLevelEnum() != null) doc.setRiskLevelEnum(dto.getRiskLevelEnum());
        if (dto.getStatusEnum() != null) doc.setStatusEnum(dto.getStatusEnum());
        if (dto.getVerificationNotes() != null) doc.setVerificationNotes(dto.getVerificationNotes());

        doc.setLastModifiedDate(LocalDateTime.now());
        CrmKycDocument saved = kycRepository.save(doc);
        return mapToData(saved);
    }

    @Override
    public void deleteDocument(Long documentId) {
        if (!kycRepository.existsById(documentId)) {
            throw new RuntimeException("Document KYC non trouvé id: " + documentId);
        }
        kycRepository.deleteById(documentId);
    }

    @Override
    public CrmKycDocumentData verifyDocument(Long documentId, Long verifiedByUserId,
            Integer riskLevelEnum, String notes) {
        CrmKycDocument doc = kycRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document KYC non trouvé id: " + documentId));

        doc.setVerifiedByUserId(verifiedByUserId);
        doc.setVerifiedOnDate(LocalDate.now());
        doc.setRiskLevelEnum(riskLevelEnum);
        doc.setVerificationNotes(notes);
        // Passe de EN_ATTENTE_VERIFICATION (3) à ACTIF (0) si la vérification réussit
        if (Integer.valueOf(3).equals(doc.getStatusEnum())) {
            doc.setStatusEnum(0);
        }
        doc.setLastModifiedDate(LocalDateTime.now());
        return mapToData(kycRepository.save(doc));
    }

    @Override
    public int markExpiredDocuments() {
        List<CrmKycDocument> expired = kycRepository.findExpiredDocuments(LocalDate.now());
        for (CrmKycDocument doc : expired) {
            doc.setStatusEnum(1); // EXPIRE
            doc.setLastModifiedDate(LocalDateTime.now());
        }
        kycRepository.saveAll(expired);
        return expired.size();
    }

    @Override
    public void migrateProspectDocumentsToClient(Long prospectId, Long clientId) {
        List<CrmKycDocument> docs = kycRepository.findByProspectId(prospectId);
        for (CrmKycDocument doc : docs) {
            doc.setClientId(clientId);
            doc.setLastModifiedDate(LocalDateTime.now());
        }
        kycRepository.saveAll(docs);
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
