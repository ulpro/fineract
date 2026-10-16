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
package org.apache.fineract.crm.prospect.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.prospect.data.CrmDuplicateMatchData;
import org.apache.fineract.crm.prospect.data.CrmProspectData;
import org.apache.fineract.crm.prospect.domain.CrmProspect;
import org.apache.fineract.crm.prospect.domain.CrmProspectRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmProspectReadPlatformServiceImpl implements CrmProspectReadPlatformService {

    private final CrmProspectRepository prospectRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<CrmProspectData> retrieveAllProspects() {
        return prospectRepository.findAll().stream()
                .map(this::mapToData)
                .collect(Collectors.toList());
    }

    @Override
    public CrmProspectData retrieveProspectById(Long prospectId) {
        CrmProspect prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new RuntimeException("Prospect non trouvé id: " + prospectId));
        return mapToData(prospect);
    }

    @Override
    public CrmDuplicateMatchData checkForDuplicates(String firstname, String lastname, String mobileNo, String email) {
        return checkForDuplicates(firstname, lastname, mobileNo, email, null, null);
    }

    @Override
    public CrmDuplicateMatchData checkForDuplicates(String firstname, String lastname, String mobileNo, String email,
            String nationalId) {
        return checkForDuplicates(firstname, lastname, mobileNo, email, nationalId, null);
    }

    @Override
    public CrmDuplicateMatchData checkForDuplicates(String firstname, String lastname, String mobileNo, String email,
            String nationalId, LocalDate dateOfBirth) {

        // 1. SF-01-03 : Dédoublonnage par numéro de pièce / NIU (priorité maximale — correspondance forte)
        if (nationalId != null && !nationalId.trim().isEmpty()) {
            List<CrmProspect> matches = prospectRepository.findByNationalId(nationalId.trim());
            if (!matches.isEmpty()) {
                CrmProspect p = matches.get(0);
                return new CrmDuplicateMatchData(true, "NATIONAL_ID", p.getId(), p.getClientId(),
                        "Doublon bloquant par numéro de pièce/NIU dans les prospects: " + nationalId);
            }
            // Vérification dans m_client (champ document_key)
            List<Long> clientMatches = jdbcTemplate.queryForList(
                    "SELECT id FROM m_client WHERE document_key = ? AND status_enum != 600 LIMIT 1",
                    Long.class, nationalId.trim());
            if (!clientMatches.isEmpty()) {
                return new CrmDuplicateMatchData(true, "NATIONAL_ID_CLIENT", null, clientMatches.get(0),
                        "Doublon bloquant par numéro de pièce/NIU dans les clients Fineract: " + nationalId);
            }
        }

        // 2. SF-01-03 : Dédoublonnage par téléphone (correspondance forte)
        if (mobileNo != null && !mobileNo.trim().isEmpty()) {
            List<CrmProspect> matches = prospectRepository.findByMobileNo(mobileNo.trim());
            if (!matches.isEmpty()) {
                CrmProspect p = matches.get(0);
                return new CrmDuplicateMatchData(true, "MOBILE", p.getId(), p.getClientId(),
                        "Doublon bloquant par téléphone dans les prospects: " + mobileNo);
            }
            List<Long> clientMatches = jdbcTemplate.queryForList(
                    "SELECT id FROM m_client WHERE mobile_no = ? AND status_enum != 600 LIMIT 1",
                    Long.class, mobileNo.trim());
            if (!clientMatches.isEmpty()) {
                return new CrmDuplicateMatchData(true, "MOBILE_CLIENT", null, clientMatches.get(0),
                        "Doublon bloquant par téléphone dans les clients Fineract: " + mobileNo);
            }
        }

        // 3. SF-01-03 : Dédoublonnage par email (correspondance forte)
        if (email != null && !email.trim().isEmpty()) {
            List<CrmProspect> matches = prospectRepository.findByEmailIgnoreCase(email.trim());
            if (!matches.isEmpty()) {
                CrmProspect p = matches.get(0);
                return new CrmDuplicateMatchData(true, "EMAIL", p.getId(), p.getClientId(),
                        "Doublon bloquant par email dans les prospects: " + email);
            }
            List<Long> clientMatches = jdbcTemplate.queryForList(
                    "SELECT id FROM m_client WHERE email_address = ? AND status_enum != 600 LIMIT 1",
                    Long.class, email.trim());
            if (!clientMatches.isEmpty()) {
                return new CrmDuplicateMatchData(true, "EMAIL_CLIENT", null, clientMatches.get(0),
                        "Doublon bloquant par email dans les clients Fineract: " + email);
            }
        }

        // 4. SF-01-03 : Dédoublonnage par nom + date de naissance (correspondance forte si les deux correspondent)
        if (firstname != null && lastname != null && dateOfBirth != null) {
            List<CrmProspect> matches = prospectRepository
                    .findByFirstnameIgnoreCaseAndLastnameIgnoreCaseAndDateOfBirth(
                            firstname.trim(), lastname.trim(), dateOfBirth);
            if (!matches.isEmpty()) {
                CrmProspect p = matches.get(0);
                return new CrmDuplicateMatchData(true, "NAME_DOB", p.getId(), p.getClientId(),
                        "Doublon bloquant par nom + date de naissance dans les prospects: "
                                + firstname + " " + lastname + " / " + dateOfBirth);
            }
            // Vérification dans m_client
            List<Long> clientMatches = jdbcTemplate.queryForList(
                    "SELECT id FROM m_client WHERE LOWER(firstname) = LOWER(?) AND LOWER(lastname) = LOWER(?) "
                    + "AND date_of_birth = ? AND status_enum != 600 LIMIT 1",
                    Long.class, firstname.trim(), lastname.trim(), dateOfBirth);
            if (!clientMatches.isEmpty()) {
                return new CrmDuplicateMatchData(true, "NAME_DOB_CLIENT", null, clientMatches.get(0),
                        "Doublon bloquant par nom + date de naissance dans les clients Fineract: "
                                + firstname + " " + lastname + " / " + dateOfBirth);
            }
        }

        // 5. SF-01-03 : Dédoublonnage par nom seul (correspondance possible — avertissement non bloquant)
        if (firstname != null && lastname != null) {
            List<CrmProspect> matches = prospectRepository.findByFirstnameIgnoreCaseAndLastnameIgnoreCase(
                    firstname.trim(), lastname.trim());
            if (!matches.isEmpty()) {
                CrmProspect p = matches.get(0);
                return new CrmDuplicateMatchData(true, "NAME_WEAK", p.getId(), p.getClientId(),
                        "Doublon possible par nom dans les prospects (vérifier manuellement): "
                                + firstname + " " + lastname);
            }
            List<Long> clientMatches = jdbcTemplate.queryForList(
                    "SELECT id FROM m_client WHERE LOWER(firstname) = LOWER(?) AND LOWER(lastname) = LOWER(?) "
                    + "AND status_enum != 600 LIMIT 1",
                    Long.class, firstname.trim(), lastname.trim());
            if (!clientMatches.isEmpty()) {
                return new CrmDuplicateMatchData(true, "NAME_WEAK_CLIENT", null, clientMatches.get(0),
                        "Doublon possible par nom dans les clients Fineract (vérifier manuellement): "
                                + firstname + " " + lastname);
            }
        }

        return new CrmDuplicateMatchData(false, "NONE", null, null, "Aucun doublon trouvé.");
    }

    private CrmProspectData mapToData(CrmProspect p) {
        return new CrmProspectData(
                p.getId(),
                p.getExternalId(),
                p.getLegalFormEnum(),
                p.getFirstname(),
                p.getLastname(),
                p.getMobileNo(),
                p.getEmail(),
                p.getDateOfBirth(),
                p.getGenderCvId(),
                p.getNationalId(),
                p.getCompanyName(),
                p.getRccmNumber(),
                p.getGroupId(),
                p.getOfficeId(),
                p.getStaffId(),
                p.getSourceCodeValueId(),
                p.getStatusEnum(),
                p.getClientId(),
                p.getNotes(),
                p.getCreatedDate()
        );
    }
}
