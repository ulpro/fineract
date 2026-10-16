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
package org.apache.fineract.crm.prospect.data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrmProspectData implements Serializable {

    private Long id;
    private String externalId;

    /**
     * legalFormEnum : 1=PERSON (personne physique), 2=ENTITY (personne morale), 3=GROUP (groupe solidaire)
     */
    private Integer legalFormEnum;

    // Champs communs
    private String firstname;
    private String lastname;
    private String mobileNo;
    private String email;

    // Personne physique
    private LocalDate dateOfBirth;
    private Long genderCvId;
    /** Numéro de pièce d'identité — pour dédoublonnage SF-01-03 */
    private String nationalId;

    // Personne morale
    private String companyName;
    private String rccmNumber;

    // Groupe solidaire
    private Long groupId;

    // Organisation
    private Long officeId;
    private Long staffId;
    private Long sourceCodeValueId;
    private Integer statusEnum;
    private Long clientId;
    private String notes;
    private LocalDateTime createdDate;
}
