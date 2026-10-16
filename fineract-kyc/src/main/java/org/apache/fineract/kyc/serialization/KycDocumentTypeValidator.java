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

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
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
public class KycDocumentTypeValidator {

    private static final Set<String> SUPPORTED_CREATE_PARAMETERS = new HashSet<>(Arrays.asList(KycApiConstants.codeParamName,
            KycApiConstants.nameParamName, KycApiConstants.descriptionParamName, KycApiConstants.activeParamName));

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public KycDocumentTypeValidator(final FromJsonHelper fromApiJsonHelper) {
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
                .resource(KycApiConstants.KYC_DOCUMENT_TYPE_RESOURCE_NAME);

        final var element = this.fromApiJsonHelper.parse(json);

        final String code = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.codeParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.codeParamName).value(code).notBlank().notExceedingLengthOf(50);

        final String name = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.nameParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.nameParamName).value(name).notBlank().notExceedingLengthOf(100);

        final String description = this.fromApiJsonHelper.extractStringNamed(KycApiConstants.descriptionParamName, element);
        baseDataValidator.reset().parameter(KycApiConstants.descriptionParamName).value(description).ignoreIfNull()
                .notExceedingLengthOf(500);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
