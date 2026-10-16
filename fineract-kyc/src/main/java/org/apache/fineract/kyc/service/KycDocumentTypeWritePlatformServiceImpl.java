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

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.kyc.api.KycApiConstants;
import org.apache.fineract.kyc.domain.KycDocumentType;
import org.apache.fineract.kyc.domain.KycDocumentTypeRepository;
import org.apache.fineract.kyc.serialization.KycDocumentTypeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class KycDocumentTypeWritePlatformServiceImpl implements KycDocumentTypeWritePlatformService {

    private final KycDocumentTypeRepository repository;
    private final KycDocumentTypeValidator validator;

    @Override
    public CommandProcessingResult createDocumentType(final JsonCommand command) {
        this.validator.validateForCreate(command.json());

        final String code = command.stringValueOfParameterNamed(KycApiConstants.codeParamName);
        final String name = command.stringValueOfParameterNamed(KycApiConstants.nameParamName);
        final String description = command.stringValueOfParameterNamed(KycApiConstants.descriptionParamName);
        final boolean active = command.parameterExists(KycApiConstants.activeParamName)
                ? command.booleanPrimitiveValueOfParameterNamed(KycApiConstants.activeParamName)
                : true;

        try {
            final KycDocumentType documentType = KycDocumentType.create(code, name, description, active);
            this.repository.saveAndFlush(documentType);

            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(documentType.getId()).build();
        } catch (final org.springframework.orm.jpa.JpaSystemException | org.springframework.dao.DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        }
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("code")) {
            final String code = command.stringValueOfParameterNamed(KycApiConstants.codeParamName);
            throw new org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException(
                    "error.msg.kyc.document.type.duplicate.code", "A document type with code '" + code + "' already exists",
                    KycApiConstants.codeParamName, code);
        }
        throw org.apache.fineract.infrastructure.core.exception.ErrorHandler.getMappable(dve,
                "error.msg.kyc.document.type.unknown.data.integrity.issue", "Unknown data integrity issue with resource.");
    }
}
