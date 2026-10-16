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
 * DTO pour CrmKycDocument — SF-01-04.
 *
 * <p>Utilisé en entrée (création/mise à jour) et en sortie (lecture API).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrmKycDocumentData {

    private Long id;
    private Long prospectId;
    private Long clientId;

    /**
     * Type de pièce :
     * 1=CNI, 2=PASSEPORT, 3=PERMIS_CONDUIRE, 4=TITRE_SEJOUR,
     * 5=RCCM, 6=NIU, 7=CARTE_VITALE, 99=AUTRE
     */
    private Integer documentTypeEnum;
    private String documentTypeLabel;

    private String documentNumber;
    private String issuingAuthority;
    private String issuingCountry;
    private LocalDate issueDate;

    /** SF-01-05 : date d'expiration (alertes 30j). */
    private LocalDate expiryDate;

    /**
     * Statut :
     * 0=ACTIF, 1=EXPIRE, 2=REVOQUE, 3=EN_ATTENTE_VERIFICATION
     */
    private Integer statusEnum;

    /** Référence interne vers le scan stocké. */
    private String fileReference;

    /** SF-01-06 : Niveau de risque LBC/FT (1=FAIBLE, 2=MOYEN, 3=ELEVE). */
    private Integer riskLevelEnum;

    private String verificationNotes;
    private Long verifiedByUserId;
    private LocalDate verifiedOnDate;

    private LocalDateTime createdDate;

    /** Indique si la pièce expire dans les 30 prochains jours (calculé). */
    private Boolean expiringWithin30Days;
}
