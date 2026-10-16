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
package org.apache.fineract.crm.kyc.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO pour CrmKycReview — SF-01-06.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrmKycReviewData {

    private Long id;
    private Long prospectId;
    private Long clientId;

    /**
     * Niveau de risque LBC/FT :
     * 1=FAIBLE, 2=MOYEN, 3=ELEVE
     */
    private Integer riskLevelEnum;

    /**
     * Statut :
     * 0=EN_COURS, 1=VALIDEE, 2=REJETEE, 3=EN_ATTENTE
     */
    private Integer statusEnum;

    private LocalDate reviewDate;
    private LocalDate nextReviewDate;
    private Long reviewedByUserId;
    private String reviewNotes;

    /**
     * Source : 0=MANUELLE, 1=AUTOMATIQUE (job scheduler)
     */
    private Integer triggerEnum;

    private LocalDateTime createdDate;

    /** Indique si cette revue est en retard (nextReviewDate dépassée). */
    private Boolean overdue;
}
