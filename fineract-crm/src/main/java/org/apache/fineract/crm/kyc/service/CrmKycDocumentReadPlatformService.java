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

import java.util.List;
import org.apache.fineract.crm.kyc.data.CrmKycDocumentData;

/**
 * SF-01-04 : Service lecture des pièces KYC.
 * SF-01-05 : Inclut la détection des pièces expirant sous 30j.
 */
public interface CrmKycDocumentReadPlatformService {

    /** Toutes les pièces d'un prospect. */
    List<CrmKycDocumentData> retrieveByProspectId(Long prospectId);

    /** Toutes les pièces d'un client Fineract. */
    List<CrmKycDocumentData> retrieveByClientId(Long clientId);

    /** Une pièce par son ID. */
    CrmKycDocumentData retrieveById(Long documentId);

    /**
     * SF-01-05 : Pièces expirant dans les prochains {@code days} jours.
     * Par défaut utilisé avec 30 jours pour les alertes.
     */
    List<CrmKycDocumentData> retrieveExpiringWithinDays(int days);

    /**
     * SF-01-05 : Pièces déjà expirées (statut ACTIF mais date dépassée).
     */
    List<CrmKycDocumentData> retrieveExpiredDocuments();
}
