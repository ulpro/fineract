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
package org.apache.fineract.crm.kyc.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CrmKycDocumentRepository extends JpaRepository<CrmKycDocument, Long> {

    /** Toutes les pièces d'un prospect. */
    List<CrmKycDocument> findByProspectId(Long prospectId);

    /** Toutes les pièces d'un client Fineract. */
    List<CrmKycDocument> findByClientId(Long clientId);

    /** Pièces par type pour un prospect. */
    List<CrmKycDocument> findByProspectIdAndDocumentTypeEnum(Long prospectId, Integer documentTypeEnum);

    /** Pièces par type pour un client. */
    List<CrmKycDocument> findByClientIdAndDocumentTypeEnum(Long clientId, Integer documentTypeEnum);

    /**
     * SF-01-05 : Pièces dont l'expiration est entre aujourd'hui et today+days.
     * Utilisé pour générer les alertes 30j avant expiration.
     */
    @Query("SELECT d FROM CrmKycDocument d WHERE d.expiryDate IS NOT NULL "
            + "AND d.expiryDate >= :today AND d.expiryDate <= :limit AND d.statusEnum = 0")
    List<CrmKycDocument> findExpiringBetween(@Param("today") LocalDate today, @Param("limit") LocalDate limit);

    /**
     * SF-01-05 : Pièces expirées (expiryDate < today et statut ACTIF).
     * Appelé par le job de mise à jour automatique du statut.
     */
    @Query("SELECT d FROM CrmKycDocument d WHERE d.expiryDate IS NOT NULL "
            + "AND d.expiryDate < :today AND d.statusEnum = 0")
    List<CrmKycDocument> findExpiredDocuments(@Param("today") LocalDate today);

    /**
     * SF-01-03 : Vérification doublon par numéro de pièce (toutes tables confondues).
     */
    List<CrmKycDocument> findByDocumentNumber(String documentNumber);
}
