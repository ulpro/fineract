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

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.kyc.api.KycApiConstants;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KycCaseValidator {

    private final FromJsonHelper fromApiJsonHelper;

    private static final Set<String> CREATE_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(KycApiConstants.clientIdParamName, KycApiConstants.tierIdParamName, "locale", "dateFormat"));

    private static final Set<String> WORKFLOW_TRANSITION_PARAMETERS = new HashSet<>(
            Arrays.asList(KycApiConstants.workflowStatusParamName, KycApiConstants.commentsParamName, "locale", "dateFormat"));

    private static final Set<String> RISK_UPDATE_PARAMETERS = new HashSet<>(
            Arrays.asList(KycApiConstants.riskLevelParamName, KycApiConstants.riskScoreParamName, KycApiConstants.riskReasonParamName,
                    KycApiConstants.riskFlagsParamName, KycApiConstants.commentsParamName, "locale", "dateFormat"));

    private static final Set<String> ASSIGNMENT_PARAMETERS = new HashSet<>(Arrays.asList(KycApiConstants.assignedAgentIdParamName,
            KycApiConstants.assignmentStatusParamName, KycApiConstants.commentsParamName, "locale", "dateFormat"));

    private static final Set<String> UPDATE_EDD_PARAMETERS = new HashSet<>(
            Arrays.asList(KycApiConstants.escalationReasonParamName, KycApiConstants.complianceNotesParamName, "locale", "dateFormat"));

    private static final Set<String> ADD_FLAG_PARAMETERS = new HashSet<>(Arrays.asList("flagTypeCode", "remarks", "locale", "dateFormat"));

    private static final Set<String> RESOLVE_FLAG_PARAMETERS = new HashSet<>(
            Arrays.asList("statusId", "resolutionRemarks", "locale", "dateFormat"));

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CREATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("kyc.case");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Long clientId = this.fromApiJsonHelper.extractLongNamed(KycApiConstants.clientIdParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.clientIdParamName).value(clientId).notNull().integerGreaterThanZero();

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.tierIdParamName, element)) {
            final Long tierId = this.fromApiJsonHelper.extractLongNamed(KycApiConstants.tierIdParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.tierIdParamName).value(tierId).notNull().integerGreaterThanZero();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForWorkflowTransition(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, WORKFLOW_TRANSITION_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("kyc.case.workflow");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Integer workflowStatus = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(KycApiConstants.workflowStatusParamName,
                element);
        baseDataValidator.reset().parameter(KycApiConstants.workflowStatusParamName).value(workflowStatus).notNull()
                .integerGreaterThanZero();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForRiskUpdate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, RISK_UPDATE_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("kyc.case.risk");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.riskLevelParamName, element)) {
            final Integer riskLevel = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(KycApiConstants.riskLevelParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.riskLevelParamName).value(riskLevel).notNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.riskScoreParamName, element)) {
            final BigDecimal riskScore = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(KycApiConstants.riskScoreParamName,
                    element);
            baseDataValidator.reset().parameter(KycApiConstants.riskScoreParamName).value(riskScore).notNull();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForAssignment(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ASSIGNMENT_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("kyc.case.assignment");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.assignedAgentIdParamName, element)) {
            final Long assignedAgentId = this.fromApiJsonHelper.extractLongNamed(KycApiConstants.assignedAgentIdParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.assignedAgentIdParamName).value(assignedAgentId).notNull()
                    .integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.assignmentStatusParamName, element)) {
            final Integer assignmentStatus = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(KycApiConstants.assignmentStatusParamName,
                    element);
            baseDataValidator.reset().parameter(KycApiConstants.assignmentStatusParamName).value(assignmentStatus).notNull()
                    .integerGreaterThanZero();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdateEdd(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, UPDATE_EDD_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("kyc.case.edd");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.escalationReasonParamName, element)) {
            final String escalationReason = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.escalationReasonParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.escalationReasonParamName).value(escalationReason)
                    .notExceedingLengthOf(500);
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.complianceNotesParamName, element)) {
            final String complianceNotes = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.complianceNotesParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.complianceNotesParamName).value(complianceNotes);
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForAddFlag(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ADD_FLAG_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("kyc.case.flag.add");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String flagTypeCode = this.fromApiJsonHelper.extractStringNamed("flagTypeCode", element);
        baseDataValidator.reset().parameter("flagTypeCode").value(flagTypeCode).notBlank().notExceedingLengthOf(50);

        if (this.fromApiJsonHelper.parameterExists("remarks", element)) {
            final String remarks = this.fromApiJsonHelper.extractStringNamed("remarks", element);
            baseDataValidator.reset().parameter("remarks").value(remarks).notExceedingLengthOf(500);
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForResolveFlag(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, RESOLVE_FLAG_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("kyc.case.flag.resolve");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Integer statusId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed("statusId", element);
        baseDataValidator.reset().parameter("statusId").value(statusId).notNull().integerGreaterThanZero();

        if (this.fromApiJsonHelper.parameterExists("resolutionRemarks", element)) {
            final String resolutionRemarks = this.fromApiJsonHelper.extractStringNamed("resolutionRemarks", element);
            baseDataValidator.reset().parameter("resolutionRemarks").value(resolutionRemarks).notExceedingLengthOf(500);
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
