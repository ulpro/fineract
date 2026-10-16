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

import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.crm.kyc.service.CrmKycDocumentWritePlatformService;
import org.apache.fineract.crm.prospect.data.CrmDuplicateMatchData;
import org.apache.fineract.crm.prospect.data.CrmProspectData;
import org.apache.fineract.crm.prospect.domain.CrmProspect;
import org.apache.fineract.crm.prospect.domain.CrmProspectRepository;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CrmProspectWritePlatformServiceImpl implements CrmProspectWritePlatformService {

    private final CrmProspectRepository prospectRepository;
    private final CrmProspectReadPlatformService prospectReadPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    /** SF-01-04 : migration des pièces KYC lors de la conversion prospect → client */
    private final CrmKycDocumentWritePlatformService kycDocumentWritePlatformService;

    @Override
    public CrmProspectData createProspect(CrmProspectData dto) {
        // SF-01-03 : Détection des doublons à la saisie (téléphone, email, nom, pièce)
        CrmDuplicateMatchData duplicateCheck = prospectReadPlatformService.checkForDuplicates(
                dto.getFirstname(), dto.getLastname(), dto.getMobileNo(), dto.getEmail(), dto.getNationalId());

        if (duplicateCheck.isDuplicateFound()) {
            throw new IllegalArgumentException("Création rejetée — Doublon détecté: " + duplicateCheck.getDetails());
        }

        String externalId = dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()
                ? dto.getExternalId()
                : "PRS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // SF-01-12 : statut par défaut = PROSPECT (0)
        Integer status = dto.getStatusEnum() != null ? dto.getStatusEnum() : 0;

        CrmProspect prospect = new CrmProspect(
                externalId,
                dto.getFirstname(),
                dto.getLastname(),
                dto.getMobileNo(),
                dto.getEmail(),
                dto.getOfficeId(),
                dto.getStaffId(),
                dto.getLegalFormEnum(),
                dto.getSourceCodeValueId(),
                status,
                dto.getNotes(),
                dto.getDateOfBirth(),
                dto.getGenderCvId(),
                dto.getNationalId(),
                dto.getCompanyName(),
                dto.getRccmNumber(),
                dto.getGroupId()
        );

        CrmProspect saved = prospectRepository.save(prospect);
        return mapToData(saved);
    }

    @Override
    public CrmProspectData updateProspect(Long prospectId, CrmProspectData dto) {
        CrmProspect prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new RuntimeException("Prospect non trouvé id: " + prospectId));

        if (dto.getFirstname() != null) prospect.setFirstname(dto.getFirstname());
        if (dto.getLastname() != null) prospect.setLastname(dto.getLastname());
        if (dto.getMobileNo() != null) prospect.setMobileNo(dto.getMobileNo());
        if (dto.getEmail() != null) prospect.setEmail(dto.getEmail());
        if (dto.getNotes() != null) prospect.setNotes(dto.getNotes());
        if (dto.getStatusEnum() != null) prospect.setStatusEnum(dto.getStatusEnum());
        if (dto.getDateOfBirth() != null) prospect.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGenderCvId() != null) prospect.setGenderCvId(dto.getGenderCvId());
        if (dto.getNationalId() != null) prospect.setNationalId(dto.getNationalId());
        if (dto.getCompanyName() != null) prospect.setCompanyName(dto.getCompanyName());
        if (dto.getRccmNumber() != null) prospect.setRccmNumber(dto.getRccmNumber());
        if (dto.getGroupId() != null) prospect.setGroupId(dto.getGroupId());
        prospect.setLastModifiedDate(LocalDateTime.now());

        CrmProspect saved = prospectRepository.save(prospect);
        return mapToData(saved);
    }

    @Override
    public CrmProspectData convertProspectToClient(Long prospectId) {
        CrmProspect prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new RuntimeException("Prospect non trouvé id: " + prospectId));

        // SF-01-02 : Conversion prospect vers client Fineract sans ressaisie
        // Construction du JSON pour créer le client dans m_client via l'API Fineract
        JsonObject clientJson = new JsonObject();
        clientJson.addProperty("officeId", prospect.getOfficeId());
        clientJson.addProperty("externalId", prospect.getExternalId());
        clientJson.addProperty("firstname", prospect.getFirstname());
        clientJson.addProperty("lastname", prospect.getLastname());

        if (prospect.getLegalFormEnum() != null) {
            clientJson.addProperty("legalFormId", prospect.getLegalFormEnum());
        }

        if (prospect.getCompanyName() != null) {
            clientJson.addProperty("fullname", prospect.getCompanyName());
        }

        if (prospect.getMobileNo() != null) {
            clientJson.addProperty("mobileNo", prospect.getMobileNo());
        }

        if (prospect.getEmail() != null) {
            clientJson.addProperty("emailAddress", prospect.getEmail());
        }

        if (prospect.getDateOfBirth() != null) {
            clientJson.addProperty("dateOfBirth", prospect.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
            clientJson.addProperty("dateFormat", "dd MMMM yyyy");
            clientJson.addProperty("locale", "en");
        }

        if (prospect.getGenderCvId() != null) {
            clientJson.addProperty("genderId", prospect.getGenderCvId());
        }

        if (prospect.getStaffId() != null) {
            clientJson.addProperty("staffId", prospect.getStaffId());
        }

        if (prospect.getGroupId() != null) {
            clientJson.addProperty("groupId", prospect.getGroupId());
        }

        // Activation immédiate du client (active = true)
        clientJson.addProperty("active", true);

        // Appel de l'API Fineract via le CommandProcessingService
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .createClient()
                .withJson(clientJson.toString())
                .build();

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        // Mise à jour du prospect avec le clientId et statut CONVERTED (2)
        // Pour createClient, l'ID du client créé est dans resourceId
        Long newClientId = result.getResourceId();
        prospect.setClientId(newClientId);
        prospect.setStatusEnum(2);
        prospect.setLastModifiedDate(LocalDateTime.now());

        // SF-01-04 : Migrer les pièces KYC du prospect vers le client Fineract créé
        kycDocumentWritePlatformService.migrateProspectDocumentsToClient(prospectId, newClientId);

        CrmProspect saved = prospectRepository.save(prospect);
        return mapToData(saved);
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
