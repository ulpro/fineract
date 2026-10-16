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
import java.time.LocalDate;
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
import org.apache.fineract.kyc.domain.KycDocumentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KycDocumentValidator {

    private static final Set<String> SUPPORTED_CREATE_PARAMETERS = new HashSet<>(Arrays.asList(KycApiConstants.documentTypeIdParamName,
            KycApiConstants.documentNumberParamName, KycApiConstants.issueDateParamName, KycApiConstants.expiryDateParamName,
            KycApiConstants.fileLocationParamName, KycApiConstants.verificationStatusParamName, KycApiConstants.remarksParamName,
            KycApiConstants.localeParamName, KycApiConstants.dateFormatParamName));

    private static final Set<String> SUPPORTED_UPDATE_PARAMETERS = new HashSet<>(Arrays.asList(KycApiConstants.documentTypeIdParamName,
            KycApiConstants.documentNumberParamName, KycApiConstants.issueDateParamName, KycApiConstants.expiryDateParamName,
            KycApiConstants.fileLocationParamName, KycApiConstants.verificationStatusParamName, KycApiConstants.remarksParamName,
            KycApiConstants.localeParamName, KycApiConstants.dateFormatParamName));

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public KycDocumentValidator(final FromJsonHelper fromApiJsonHelper) {
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
                .resource(KycApiConstants.KYC_DOCUMENT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Long documentTypeId = this.fromApiJsonHelper.extractLongNamed(KycApiConstants.documentTypeIdParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.documentTypeIdParamName).value(documentTypeId).notNull().longGreaterThanZero();

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.documentNumberParamName, element)) {
            final String documentNumber = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.documentNumberParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.documentNumberParamName).value(documentNumber).notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.issueDateParamName, element)) {
            final LocalDate issueDate = this.fromApiJsonHelper.extractLocalDateNamed(KycApiConstants.issueDateParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.issueDateParamName).value(issueDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.expiryDateParamName, element)) {
            final LocalDate expiryDate = this.fromApiJsonHelper.extractLocalDateNamed(KycApiConstants.expiryDateParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.expiryDateParamName).value(expiryDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.fileLocationParamName, element)) {
            final String fileLocation = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.fileLocationParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.fileLocationParamName).value(fileLocation).notExceedingLengthOf(500);
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.verificationStatusParamName, element)) {
            final Integer status = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(KycApiConstants.verificationStatusParamName,
                    element);
            baseDataValidator.reset().parameter(KycApiConstants.verificationStatusParamName).value(status).notNull()
                    .inMinMaxRange(KycDocumentStatus.PENDING.getValue(), KycDocumentStatus.EXPIRED.getValue());
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.remarksParamName, element)) {
            final String remarks = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.remarksParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.remarksParamName).value(remarks).notExceedingLengthOf(1000);
        }

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
                .resource(KycApiConstants.KYC_DOCUMENT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.documentTypeIdParamName, element)) {
            final Long documentTypeId = this.fromApiJsonHelper.extractLongNamed(KycApiConstants.documentTypeIdParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.documentTypeIdParamName).value(documentTypeId).notNull()
                    .longGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.documentNumberParamName, element)) {
            final String documentNumber = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.documentNumberParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.documentNumberParamName).value(documentNumber).notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.issueDateParamName, element)) {
            final LocalDate issueDate = this.fromApiJsonHelper.extractLocalDateNamed(KycApiConstants.issueDateParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.issueDateParamName).value(issueDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.expiryDateParamName, element)) {
            final LocalDate expiryDate = this.fromApiJsonHelper.extractLocalDateNamed(KycApiConstants.expiryDateParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.expiryDateParamName).value(expiryDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.fileLocationParamName, element)) {
            final String fileLocation = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.fileLocationParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.fileLocationParamName).value(fileLocation).notExceedingLengthOf(500);
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.verificationStatusParamName, element)) {
            final Integer status = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(KycApiConstants.verificationStatusParamName,
                    element);
            baseDataValidator.reset().parameter(KycApiConstants.verificationStatusParamName).value(status).notNull()
                    .inMinMaxRange(KycDocumentStatus.PENDING.getValue(), KycDocumentStatus.EXPIRED.getValue());
        }

        if (this.fromApiJsonHelper.parameterExists(KycApiConstants.remarksParamName, element)) {
            final String remarks = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.remarksParamName, element);
            baseDataValidator.reset().parameter(KycApiConstants.remarksParamName).value(remarks).notExceedingLengthOf(1000);
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
