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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SF-01-06 : Revue KYC périodique LBC/FT.
 *
 * <p>risk_level_enum : 1=FAIBLE, 2=MOYEN, 3=ELEVE
 * <p>status_enum     : 0=EN_COURS, 1=VALIDEE, 2=REJETEE, 3=EN_ATTENTE
 * <p>trigger_enum    : 0=MANUELLE, 1=AUTOMATIQUE
 */
@Entity
@Table(name = "crm_kyc_review")
@Getter
@Setter
@NoArgsConstructor
public class CrmKycReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prospect_id")
    private Long prospectId;

    @Column(name = "client_id")
    private Long clientId;

    /** Niveau de risque LBC/FT : 1=FAIBLE, 2=MOYEN, 3=ELEVE */
    @Column(name = "risk_level_enum", nullable = false)
    private Integer riskLevelEnum;

    /** Statut : 0=EN_COURS, 1=VALIDEE, 2=REJETEE, 3=EN_ATTENTE */
    @Column(name = "status_enum", nullable = false)
    private Integer statusEnum = 0;

    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;

    /** Prochaine revue planifiée (calculée selon riskLevelEnum). */
    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    /** Source : 0=MANUELLE, 1=AUTOMATIQUE (job scheduler) */
    @Column(name = "trigger_enum", nullable = false)
    private Integer triggerEnum = 0;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "lastmodified_date")
    private LocalDateTime lastModifiedDate;

    public CrmKycReview(Long prospectId, Long clientId, Integer riskLevelEnum,
            LocalDate reviewDate, LocalDate nextReviewDate, Long reviewedByUserId,
            String reviewNotes, Integer triggerEnum) {
        this.prospectId = prospectId;
        this.clientId = clientId;
        this.riskLevelEnum = riskLevelEnum;
        this.reviewDate = reviewDate;
        this.nextReviewDate = nextReviewDate;
        this.reviewedByUserId = reviewedByUserId;
        this.reviewNotes = reviewNotes;
        this.triggerEnum = triggerEnum != null ? triggerEnum : 0;
        this.statusEnum = 0;
        this.createdDate = LocalDateTime.now();
    }

    /**
     * Calcule la prochaine date de revue selon le niveau de risque.
     * - FAIBLE (1) : +12 mois
     * - MOYEN  (2) : +6 mois
     * - ELEVE  (3) : +3 mois
     */
    public static LocalDate computeNextReviewDate(LocalDate from, Integer riskLevelEnum) {
        if (from == null) {
            from = LocalDate.now();
        }
        if (Integer.valueOf(3).equals(riskLevelEnum)) {
            return from.plusMonths(3);
        } else if (Integer.valueOf(2).equals(riskLevelEnum)) {
            return from.plusMonths(6);
        }
        return from.plusMonths(12);
    }
}
