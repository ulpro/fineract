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

import org.apache.fineract.crm.kyc.data.CrmKycReviewData;

/**
 * SF-01-06 : Service écriture des revues KYC LBC/FT.
 */
public interface CrmKycReviewWritePlatformService {

    /**
     * Créer une nouvelle revue KYC pour un prospect ou un client.
     * Calcule automatiquement la prochaine date de revue selon le niveau de risque.
     */
    CrmKycReviewData createReview(CrmKycReviewData dto);

    /**
     * Valider une revue (statut 0/3 → 1) et mettre à jour le niveau de risque
     * sur le prospect/client.
     */
    CrmKycReviewData validateReview(Long reviewId, Integer riskLevelEnum, String notes, Long validatedByUserId);

    /**
     * Rejeter une revue (statut → 2).
     */
    CrmKycReviewData rejectReview(Long reviewId, String notes, Long rejectedByUserId);

    /**
     * Mettre à jour le niveau de risque global sur un prospect.
     * Appelé après validation d'une revue.
     */
    void updateProspectRiskLevel(Long prospectId, Integer riskLevelEnum);
}
