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
package org.apache.fineract.kyc.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.kyc.api.KycApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KycTierValidator {

    private static final Set<String> SUPPORTED_CREATE_PARAMETERS = new HashSet<>(Arrays.asList(KycApiConstants.codeParamName,
            KycApiConstants.nameParamName, KycApiConstants.descriptionParamName, KycApiConstants.levelParamName,
            KycApiConstants.maxBalanceParamName, KycApiConstants.maxTransactionAmountParamName, KycApiConstants.activeParamName,
            KycApiConstants.validityDaysParamName, KycApiConstants.clientTypeCvIdParamName, KycApiConstants.documentRequirementsParamName,
            KycApiConstants.requiredFieldsParamName, KycApiConstants.localeParamName, KycApiConstants.dateFormatParamName));

    private static final Set<String> SUPPORTED_UPDATE_PARAMETERS = new HashSet<>(Arrays.asList(KycApiConstants.nameParamName,
            KycApiConstants.descriptionParamName, KycApiConstants.levelParamName, KycApiConstants.maxBalanceParamName,
            KycApiConstants.maxTransactionAmountParamName, KycApiConstants.activeParamName, KycApiConstants.validityDaysParamName,
            KycApiConstants.clientTypeCvIdParamName, KycApiConstants.documentRequirementsParamName, KycApiConstants.requiredFieldsParamName,
            KycApiConstants.localeParamName, KycApiConstants.dateFormatParamName));

    private static final Set<String> SUPPORTED_DOCUMENT_REQUIREMENT_PARAMETERS = new HashSet<>(
            Arrays.asList(KycApiConstants.documentTypeIdParamName, KycApiConstants.mandatoryParamName));

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public KycTierValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SUPPORTED_CREATE_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(KycApiConstants.KYC_TIER_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String code = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.codeParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.codeParamName).value(code).notBlank().notExceedingLengthOf(50);

        final String name = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.nameParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.nameParamName).value(name).notBlank().notExceedingLengthOf(100);

        final Integer level = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(KycApiConstants.levelParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.levelParamName).value(level).notNull().integerGreaterThanZero();

        final BigDecimal maxBalance = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(KycApiConstants.maxBalanceParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.maxBalanceParamName).value(maxBalance).ignoreIfNull().positiveAmount();

        final BigDecimal maxTxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(KycApiConstants.maxTransactionAmountParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.maxTransactionAmountParamName).value(maxTxAmount).ignoreIfNull()
                .positiveAmount();

        final Integer validityDays = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(KycApiConstants.validityDaysParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.validityDaysParamName).value(validityDays).ignoreIfNull()
                .integerGreaterThanZero();

        final Long clientTypeCvId = this.fromApiJsonHelper.extractLongNamed(KycApiConstants.clientTypeCvIdParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.clientTypeCvIdParamName).value(clientTypeCvId).ignoreIfNull()
                .longGreaterThanZero();

        validateDocumentRequirements(element, typeOfMap, dataValidationErrors);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SUPPORTED_UPDATE_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(KycApiConstants.KYC_TIER_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.nameParamName, element)) {
            final String name = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.nameParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.nameParamName).value(name).notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.levelParamName, element)) {
            final Integer level = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(KycApiConstants.levelParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.levelParamName).value(level).notNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.maxBalanceParamName, element)) {
            final BigDecimal maxBalance = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(KycApiConstants.maxBalanceParamName,
                    element);
            baseDataValidator.reset().parameter(KycApiConstants.maxBalanceParamName).value(maxBalance).ignoreIfNull().positiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.maxTransactionAmountParamName, element)) {
            final BigDecimal maxTxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(KycApiConstants.maxTransactionAmountParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.maxTransactionAmountParamName).value(maxTxAmount).ignoreIfNull()
                    .positiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.validityDaysParamName, element)) {
            final Integer validityDays = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(KycApiConstants.validityDaysParamName,
                    element);
            baseDataValidator.reset().parameter(KycApiConstants.validityDaysParamName).value(validityDays).ignoreIfNull()
                    .integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.clientTypeCvIdParamName, element)) {
            final Long clientTypeCvId = this.fromApiJsonHelper.extractLongNamed(KycApiConstants.clientTypeCvIdParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.clientTypeCvIdParamName).value(clientTypeCvId).ignoreIfNull()
                    .longGreaterThanZero();
        }

        validateDocumentRequirements(element, typeOfMap, dataValidationErrors);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void validateDocumentRequirements(final JsonElement element, final Type typeOfMap,
            final List<ApiParameterError> dataValidationErrors) {
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(KycApiConstants.KYC_TIER_RESOURCE_NAME);

        if (!this.fromApiJsonHelper.parameterExists(KycApiConstants.documentRequirementsParamName, element)) {
            return;
        }

        final JsonArray requirements = this.fromApiJsonHelper.extractJsonArrayNamed(KycApiConstants.documentRequirementsParamName, element);
        if (requirements == null) {
            return;
        }

        for (int i = 1; i <= requirements.size(); i++) {
            final JsonObject req = requirements.get(i - 1).getAsJsonObject();
            final String reqJson = this.fromApiJsonHelper.toJson(req);
            this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, reqJson, SUPPORTED_DOCUMENT_REQUIREMENT_PARAMETERS);

            final Long documentTypeId = this.fromApiJsonHelper.extractLongNamed(KycApiConstants.documentTypeIdParamName, req);
            baseDataValidator.reset().parameter(KycApiConstants.documentRequirementsParamName)
                    .parameterAtIndexArray(KycApiConstants.documentTypeIdParamName, i).value(documentTypeId).notNull()
                    .longGreaterThanZero();
        }
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
