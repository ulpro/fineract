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
package org.apache.fineract.crm.kyc.jobs;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.crm.kyc.data.CrmKycDocumentData;
import org.apache.fineract.crm.kyc.service.CrmKycDocumentReadPlatformService;
import org.apache.fineract.crm.kyc.service.CrmKycDocumentWritePlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * SF-01-05 : Job schedulé quotidien pour la gestion de l'expiration des pièces KYC.
 *
 * <p>Actions :
 * <ol>
 *   <li>Marque les pièces dont la date d'expiration est dépassée (statut ACTIF → EXPIRE).</li>
 *   <li>Logue les pièces expirant dans les 30 prochains jours pour déclenchement d'alertes.</li>
 * </ol>
 *
 * <p>Les alertes proprement dites (notification push, SMS, email) sont traitées par les
 * modules de notification Fineract — ce tasklet se charge uniquement de la mise à jour
 * du statut et du log d'avertissement.
 */
@Slf4j
@AllArgsConstructor
@Component
public class UpdateKycDocumentExpirationTasklet implements Tasklet {

    private final CrmKycDocumentWritePlatformService kycWriteService;
    private final CrmKycDocumentReadPlatformService kycReadService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        try {
            // 1. Marquer les pièces expirées (statut 0 → 1)
            int markedExpired = kycWriteService.markExpiredDocuments();
            if (markedExpired > 0) {
                log.info("SF-01-05 KYC : {} pièce(s) marquée(s) comme expirée(s).", markedExpired);
            }

            // 2. Logger les pièces qui expirent dans les 30 prochains jours (avertissements)
            List<CrmKycDocumentData> expiringSoon = kycReadService.retrieveExpiringWithinDays(30);
            if (!expiringSoon.isEmpty()) {
                log.warn("SF-01-05 KYC : {} pièce(s) expire(nt) dans les 30 prochains jours — "
                        + "vérifier les alertes clients.", expiringSoon.size());
                for (CrmKycDocumentData doc : expiringSoon) {
                    log.warn("  → KYC id={} type={} numero={} prospect={} client={} expiration={}",
                            doc.getId(), doc.getDocumentTypeEnum(), doc.getDocumentNumber(),
                            doc.getProspectId(), doc.getClientId(), doc.getExpiryDate());
                }
            }
        } catch (Exception e) {
            log.error("SF-01-05 KYC : Erreur lors de la mise à jour de l'expiration des pièces KYC.", e);
        }
        return RepeatStatus.FINISHED;
    }
}
