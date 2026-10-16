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
package org.apache.fineract.kyc.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.kyc.api.KycApiConstants;
import org.apache.fineract.kyc.domain.KycDocumentType;
import org.apache.fineract.kyc.domain.KycDocumentTypeRepositoryWrapper;
import org.apache.fineract.kyc.domain.KycTier;
import org.apache.fineract.kyc.domain.KycTierDocumentRequirement;
import org.apache.fineract.kyc.domain.KycTierRepository;
import org.apache.fineract.kyc.domain.KycTierRepositoryWrapper;
import org.apache.fineract.kyc.domain.KycTierRequiredField;
import org.apache.fineract.kyc.serialization.KycTierValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class KycTierWritePlatformServiceImpl implements KycTierWritePlatformService {

    private final KycTierRepository repository;
    private final KycTierRepositoryWrapper repositoryWrapper;
    private final KycDocumentTypeRepositoryWrapper documentTypeRepositoryWrapper;
    private final KycTierValidator validator;
    private final FromJsonHelper fromApiJsonHelper;

    @Override
    public CommandProcessingResult createTier(final JsonCommand command) {
        this.validator.validateForCreate(command.json());

        final String code = command.stringValueOfParameterNamed(KycApiConstants.codeParamName);
        final String name = command.stringValueOfParameterNamed(KycApiConstants.nameParamName);
        final String description = command.stringValueOfParameterNamed(KycApiConstants.descriptionParamName);
        final Integer level = command.integerValueOfParameterNamed(KycApiConstants.levelParamName);
        final BigDecimal maxBalance = command.bigDecimalValueOfParameterNamed(KycApiConstants.maxBalanceParamName);
        final BigDecimal maxTxAmount = command.bigDecimalValueOfParameterNamed(KycApiConstants.maxTransactionAmountParamName);
        final boolean active = command.parameterExists(KycApiConstants.activeParamName)
                ? command.booleanPrimitiveValueOfParameterNamed(KycApiConstants.activeParamName)
                : true;
        final Integer validityDays = command.integerValueOfParameterNamed("validityDays");
        final Long clientTypeCvId = command.longValueOfParameterNamed("clientTypeCvId");

        final KycTier tier = KycTier.create(code, name, description, level, maxBalance, maxTxAmount, active, validityDays, clientTypeCvId);
        this.repository.saveAndFlush(tier);

        final Set<KycTierDocumentRequirement> requirements = buildDocumentRequirements(command, tier);
        tier.updateDocumentRequirements(requirements);

        final Set<KycTierRequiredField> requiredFields = buildRequiredFields(command, tier);
        tier.updateRequiredFields(requiredFields);

        this.repository.saveAndFlush(tier);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(tier.getId()).build();
    }

    @Override
    public CommandProcessingResult updateTier(final Long id, final JsonCommand command) {
        this.validator.validateForUpdate(command.json());

        final KycTier tier = this.repositoryWrapper.findOneWithNotFoundDetection(id);
        final Map<String, Object> changes = tier.update(command);

        if (command.parameterExists(KycApiConstants.documentRequirementsParamName)) {
            final Set<KycTierDocumentRequirement> requirements = buildDocumentRequirements(command, tier);
            tier.updateDocumentRequirements(requirements);
            changes.put(KycApiConstants.documentRequirementsParamName, "updated");
        }

        if (command.parameterExists("requiredFields")) {
            final Set<KycTierRequiredField> requiredFields = buildRequiredFields(command, tier);
            tier.updateRequiredFields(requiredFields);
            changes.put("requiredFields", "updated");
        }

        if (!changes.isEmpty()) {
            this.repository.saveAndFlush(tier);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(id).with(changes).build();
    }

    private Set<KycTierDocumentRequirement> buildDocumentRequirements(final JsonCommand command, final KycTier tier) {
        final Set<KycTierDocumentRequirement> requirements = new HashSet<>();
        final JsonElement element = this.fromApiJsonHelper.parse(command.json());
        if (!this.fromApiJsonHelper.parameterExists(KycApiConstants.documentRequirementsParamName, element)) {
            return requirements;
        }
        final JsonArray reqArray = this.fromApiJsonHelper.extractJsonArrayNamed(KycApiConstants.documentRequirementsParamName, element);
        if (reqArray == null) {
            return requirements;
        }
        for (int i = 0; i < reqArray.size(); i++) {
            final JsonObject reqObj = reqArray.get(i).getAsJsonObject();
            final Long docTypeId = this.fromApiJsonHelper.extractLongNamed(KycApiConstants.documentTypeIdParamName, reqObj);
            final boolean mandatory = reqObj.has(KycApiConstants.mandatoryParamName)
                    ? this.fromApiJsonHelper.extractBooleanNamed(KycApiConstants.mandatoryParamName, reqObj)
                    : true;
            final KycDocumentType docType = this.documentTypeRepositoryWrapper.findOneWithNotFoundDetection(docTypeId);
            requirements.add(new KycTierDocumentRequirement(tier, docType, mandatory));
        }
        return requirements;
    }

    private Set<KycTierRequiredField> buildRequiredFields(final JsonCommand command, final KycTier tier) {
        final Set<KycTierRequiredField> requiredFields = new HashSet<>();
        final JsonElement element = this.fromApiJsonHelper.parse(command.json());
        if (!this.fromApiJsonHelper.parameterExists("requiredFields", element)) {
            return requiredFields;
        }
        final JsonArray reqArray = this.fromApiJsonHelper.extractJsonArrayNamed("requiredFields", element);
        if (reqArray == null) {
            return requiredFields;
        }
        for (int i = 0; i < reqArray.size(); i++) {
            final String fieldName = reqArray.get(i).getAsString();
            requiredFields.add(KycTierRequiredField.create(tier, fieldName));
        }
        return requiredFields;
    }
}
