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
package org.apache.fineract.crm.snapshot360.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.complaint.service.CrmComplaintReadPlatformService;
import org.apache.fineract.crm.consent.domain.CrmConsentRepository;
import org.apache.fineract.crm.kyc.service.CrmKycReviewReadPlatformService;
import org.apache.fineract.crm.prospect.service.CrmProspectReadPlatformService;
import org.apache.fineract.crm.snapshot360.data.CrmClient360Data;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmClient360ReadPlatformServiceImpl implements CrmClient360ReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final CrmConsentRepository consentRepository;
    private final CrmComplaintReadPlatformService complaintReadService;
    private final CrmKycReviewReadPlatformService kycReviewReadService;
    private final CrmProspectReadPlatformService prospectReadService;

    @Override
    public CrmClient360Data retrieveClient360View(Long clientId) {
        // 1. Infos client Fineract (Standard)
        String sql = "SELECT firstname, lastname, status_enum, account_no FROM m_client WHERE id = ?";
        List<Object[]> client = jdbcTemplate.query(sql, (rs, rowNum) -> new Object[] {
                rs.getString("firstname"), rs.getString("lastname"),
                rs.getInt("status_enum"), rs.getString("account_no")
        }, clientId);

        if (client.isEmpty()) {
            throw new RuntimeException("Client non trouvé id: " + clientId);
        }
        Object[] row = client.get(0);

        CrmClient360Data data = new CrmClient360Data();
        data.setClientId(clientId);
        data.setFirstname((String) row[0]);
        data.setLastname((String) row[1]);
        data.setStatus(row[2].toString());
        data.setClientAccountNo((String) row[3]);

        // 2. Réclamations (CRM)
        data.setOpenComplaintsCount(complaintReadService.retrieveAllComplaints().stream()
                .filter(c -> c.getClientId() != null && c.getClientId().equals(clientId) && c.getStatusEnum() != 2) // Non résolu
                .mapToInt(c -> 1).sum());

        // 3. KYC (Risk level) - Prend le niveau de risque de la dernière revue
        data.setRiskLevel("NON_EVALUE");
        kycReviewReadService.retrieveByClientId(clientId).stream().findFirst()
                .ifPresent(r -> data.setRiskLevel(r.getRiskLevelEnum().toString()));

        // 4. Comptes Fineract (Loans/Savings)
        data.setActiveLoansCount(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM m_loan WHERE client_id = ? AND loan_status_id = 300", Integer.class, clientId));
        data.setActiveSavingsCount(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM m_savings_account WHERE client_id = ? AND status_enum = 300", Integer.class, clientId));

        // 5. Consentements
        data.setConsents(consentRepository.findByClientId(clientId));

        // 6. Historique prospects (pas de lien direct m_client vers crm_prospect, à mapper via externalId à terme)
        data.setProspectHistory(new java.util.ArrayList<>());

        // Score complétude KYC simple
        data.setKycCompletenessScore(calculateKycScore(clientId));

        return data;
    }

    private int calculateKycScore(Long clientId) {
        // Logique simplifiée : 100% si au moins un document et une revue
        int hasDocuments = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM crm_kyc_document WHERE client_id = ?", Integer.class, clientId);
        int hasReview = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM crm_kyc_review WHERE client_id = ?", Integer.class, clientId);
        return (hasDocuments > 0 && hasReview > 0) ? 100 : 50;
    }
}
