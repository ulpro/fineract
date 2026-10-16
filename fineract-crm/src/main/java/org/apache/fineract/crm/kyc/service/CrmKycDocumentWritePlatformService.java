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

import org.apache.fineract.crm.kyc.data.CrmKycDocumentData;

/**
 * SF-01-04 : Service écriture des pièces KYC.
 */
public interface CrmKycDocumentWritePlatformService {

    /** Ajouter une pièce KYC à un prospect ou un client. */
    CrmKycDocumentData createDocument(CrmKycDocumentData dto);

    /** Mettre à jour une pièce existante (numéro, dates, scan, etc.). */
    CrmKycDocumentData updateDocument(Long documentId, CrmKycDocumentData dto);

    /** Supprimer une pièce KYC. */
    void deleteDocument(Long documentId);

    /**
     * SF-01-06 : Marquer une pièce comme vérifiée et affecter un niveau de risque LBC/FT.
     *
     * @param documentId    ID de la pièce
     * @param verifiedByUserId  ID de l'utilisateur vérifiant
     * @param riskLevelEnum 1=FAIBLE, 2=MOYEN, 3=ELEVE
     * @param notes         observations de vérification
     */
    CrmKycDocumentData verifyDocument(Long documentId, Long verifiedByUserId, Integer riskLevelEnum, String notes);

    /**
     * SF-01-05 : Mettre à jour le statut des pièces expirées (statut 0→1).
     * Appelé par le scheduler quotidien.
     *
     * @return nombre de pièces mises à jour
     */
    int markExpiredDocuments();

    /**
     * Migrer les pièces d'un prospect vers un client après conversion (SF-01-02).
     * Lie toutes les pièces du prospect au clientId Fineract.
     */
    void migrateProspectDocumentsToClient(Long prospectId, Long clientId);
}
