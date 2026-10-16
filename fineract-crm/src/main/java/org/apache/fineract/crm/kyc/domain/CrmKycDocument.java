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
 * SF-01-04 : Pièce justificative KYC d'un prospect ou d'un client.
 *
 * <p>document_type_enum :
 * 1=CNI, 2=PASSEPORT, 3=PERMIS_CONDUIRE, 4=TITRE_SEJOUR,
 * 5=RCCM, 6=NIU, 7=CARTE_VITALE, 99=AUTRE
 *
 * <p>status_enum :
 * 0=ACTIF, 1=EXPIRE, 2=REVOQUE, 3=EN_ATTENTE_VERIFICATION
 */
@Entity
@Table(name = "crm_kyc_document")
@Getter
@Setter
@NoArgsConstructor
public class CrmKycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lien vers le prospect CRM (avant conversion). Null si déjà converti. */
    @Column(name = "prospect_id")
    private Long prospectId;

    /** Lien vers le client Fineract (après conversion, ou saisie directe). */
    @Column(name = "client_id")
    private Long clientId;

    /** Type de pièce (enum interne CRM). */
    @Column(name = "document_type_enum", nullable = false)
    private Integer documentTypeEnum;

    /** Libellé libre si documentTypeEnum = 99 (AUTRE). */
    @Column(name = "document_type_label", length = 100)
    private String documentTypeLabel;

    /** Numéro de la pièce (CNI: 12345678, Passeport: AB123456, etc.). */
    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;

    /** Autorité émettrice (ex : Préfecture de Douala). */
    @Column(name = "issuing_authority", length = 200)
    private String issuingAuthority;

    /** Code ISO 2 lettres du pays d'émission. */
    @Column(name = "issuing_country", length = 2)
    private String issuingCountry;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    /** SF-01-05 : Date d'expiration — utilisée pour les alertes 30j. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** Statut du document (0=ACTIF, 1=EXPIRE, 2=REVOQUE, 3=EN_ATTENTE). */
    @Column(name = "status_enum", nullable = false)
    private Integer statusEnum = 0;

    /** Référence interne vers le fichier scanné (path ou URL interne). */
    @Column(name = "file_reference", length = 500)
    private String fileReference;

    /** SF-01-06 : Niveau de risque LBC/FT (1=FAIBLE, 2=MOYEN, 3=ELEVE). */
    @Column(name = "risk_level_enum")
    private Integer riskLevelEnum;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "verified_by_user_id")
    private Long verifiedByUserId;

    @Column(name = "verified_on_date")
    private LocalDate verifiedOnDate;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "lastmodified_date")
    private LocalDateTime lastModifiedDate;

    public CrmKycDocument(Long prospectId, Long clientId, Integer documentTypeEnum,
            String documentTypeLabel, String documentNumber, String issuingAuthority,
            String issuingCountry, LocalDate issueDate, LocalDate expiryDate,
            String fileReference) {
        this.prospectId = prospectId;
        this.clientId = clientId;
        this.documentTypeEnum = documentTypeEnum;
        this.documentTypeLabel = documentTypeLabel;
        this.documentNumber = documentNumber;
        this.issuingAuthority = issuingAuthority;
        this.issuingCountry = issuingCountry;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.fileReference = fileReference;
        this.statusEnum = 0;
        this.createdDate = LocalDateTime.now();
    }
}
