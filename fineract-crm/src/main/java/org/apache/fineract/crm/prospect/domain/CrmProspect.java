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
package org.apache.fineract.crm.prospect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "crm_prospect")
@Getter
@Setter
@NoArgsConstructor
public class CrmProspect extends AbstractPersistableCustom<Long> {

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    /**
     * legalFormEnum : 1=PERSON (personne physique), 2=ENTITY (personne morale), 3=GROUP (groupe solidaire)
     */
    @Column(name = "legal_form_enum")
    private Integer legalFormEnum;

    // --- Champs communs ---
    @Column(name = "firstname", nullable = false, length = 100)
    private String firstname;

    @Column(name = "lastname", nullable = false, length = 100)
    private String lastname;

    @Column(name = "mobile_no", length = 50)
    private String mobileNo;

    @Column(name = "email", length = 100)
    private String email;

    // --- Personne physique ---
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender_cv_id")
    private Long genderCvId;

    /** Numéro de pièce d'identité (CNI, passeport, NIU…) — utilisé pour dédoublonnage SF-01-03 */
    @Column(name = "national_id", length = 100)
    private String nationalId;

    // --- Personne morale ---
    @Column(name = "company_name", length = 200)
    private String companyName;

    /** Numéro RCCM pour les personnes morales */
    @Column(name = "rccm_number", length = 50)
    private String rccmNumber;

    // --- Groupe solidaire ---
    /** ID du groupe Fineract si le prospect est rattaché à un groupe solidaire */
    @Column(name = "group_id")
    private Long groupId;

    // --- Organisation ---
    @Column(name = "office_id")
    private Long officeId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "source_cv_id")
    private Long sourceCodeValueId;

    @Column(name = "status_enum", nullable = false)
    private Integer statusEnum;

    /** Lien vers le client Fineract après conversion (SF-01-02) */
    @Column(name = "client_id")
    private Long clientId;

    // --- KYC / LBC-FT ---
    /** SF-01-06 : Niveau de risque LBC/FT (1=FAIBLE, 2=MOYEN, 3=ELEVE) */
    @Column(name = "risk_level_enum")
    private Integer riskLevelEnum;

    /** SF-01-06 : Date de la dernière revue KYC */
    @Column(name = "last_kyc_review_date")
    private LocalDate lastKycReviewDate;

    /** SF-01-06 : Date de la prochaine revue KYC (alertes via SF-01-05) */
    @Column(name = "next_kyc_review_date")
    private LocalDate nextKycReviewDate;

    @Column(name = "notes")
    private String notes;

    @Column(name = "createdby_id")
    private Long createdById;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "lastmodifiedby_id")
    private Long lastModifiedById;

    @Column(name = "lastmodified_date")
    private LocalDateTime lastModifiedDate;

    public CrmProspect(String externalId, String firstname, String lastname, String mobileNo, String email,
            Long officeId, Long staffId, Integer legalFormEnum, Long sourceCodeValueId, Integer statusEnum,
            String notes, LocalDate dateOfBirth, Long genderCvId, String nationalId,
            String companyName, String rccmNumber, Long groupId) {
        this.externalId = externalId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.mobileNo = mobileNo;
        this.email = email;
        this.officeId = officeId;
        this.staffId = staffId;
        this.legalFormEnum = legalFormEnum;
        this.sourceCodeValueId = sourceCodeValueId;
        this.statusEnum = statusEnum;
        this.notes = notes;
        this.dateOfBirth = dateOfBirth;
        this.genderCvId = genderCvId;
        this.nationalId = nationalId;
        this.companyName = companyName;
        this.rccmNumber = rccmNumber;
        this.groupId = groupId;
        this.createdDate = LocalDateTime.now();
        this.lastModifiedDate = LocalDateTime.now();
    }
}
