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

import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.kyc.api.KycApiConstants;
import org.apache.fineract.kyc.domain.KycDocument;
import org.apache.fineract.kyc.domain.KycDocumentRepository;
import org.apache.fineract.kyc.domain.KycDocumentRepositoryWrapper;
import org.apache.fineract.kyc.domain.KycDocumentStatus;
import org.apache.fineract.kyc.domain.KycDocumentType;
import org.apache.fineract.kyc.domain.KycDocumentTypeRepositoryWrapper;
import org.apache.fineract.kyc.serialization.KycDocumentValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class KycDocumentWritePlatformServiceImpl implements KycDocumentWritePlatformService {

    private final KycDocumentRepository repository;
    private final KycDocumentRepositoryWrapper repositoryWrapper;
    private final KycDocumentTypeRepositoryWrapper documentTypeRepositoryWrapper;
    private final KycDocumentValidator validator;
    private final KycEvaluationService evaluationService;

    @Override
    public CommandProcessingResult createDocument(final Long clientId, final JsonCommand command) {
        this.validator.validateForCreate(command.json());

        final Long documentTypeId = command.longValueOfParameterNamed(KycApiConstants.documentTypeIdParamName);
        final KycDocumentType documentType = this.documentTypeRepositoryWrapper.findOneWithNotFoundDetection(documentTypeId);

        final String documentNumber = command.stringValueOfParameterNamed(KycApiConstants.documentNumberParamName);
        final LocalDate issueDate = command.localDateValueOfParameterNamed(KycApiConstants.issueDateParamName);
        final LocalDate expiryDate = command.localDateValueOfParameterNamed(KycApiConstants.expiryDateParamName);
        final String fileLocation = command.stringValueOfParameterNamed(KycApiConstants.fileLocationParamName);

        KycDocumentStatus status = KycDocumentStatus.PENDING;
        if (command.parameterExists(KycApiConstants.verificationStatusParamName)) {
            status = KycDocumentStatus.fromInt(command.integerValueOfParameterNamed(KycApiConstants.verificationStatusParamName));
        }

        final String remarks = command.stringValueOfParameterNamed(KycApiConstants.remarksParamName);

        final KycDocument document = KycDocument.create(clientId, documentType, documentNumber, issueDate, expiryDate, fileLocation, status,
                remarks);
        this.repository.saveAndFlush(document);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(clientId).withEntityId(document.getId())
                .build();
    }

    @Override
    public CommandProcessingResult updateDocument(final Long clientId, final Long documentId, final JsonCommand command) {
        this.validator.validateForUpdate(command.json());

        final KycDocument document = this.repositoryWrapper.findOneWithNotFoundDetection(documentId);

        KycDocumentType documentType = null;
        if (command.parameterExists(KycApiConstants.documentTypeIdParamName)) {
            final Long documentTypeId = command.longValueOfParameterNamed(KycApiConstants.documentTypeIdParamName);
            documentType = this.documentTypeRepositoryWrapper.findOneWithNotFoundDetection(documentTypeId);
        }

        final Map<String, Object> changes = document.update(command, documentType);

        if (!changes.isEmpty()) {
            this.repository.saveAndFlush(document);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(clientId).withEntityId(documentId)
                .with(changes).build();
    }

    @Override
    public CommandProcessingResult submitDocument(final Long documentId, final JsonCommand command) {
        final KycDocument document = this.repositoryWrapper.findOneWithNotFoundDetection(documentId);

        final Map<String, Object> changes = document.submit();
        if (!changes.isEmpty()) {
            this.repository.saveAndFlush(document);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(document.getClientId())
                .withEntityId(documentId).with(changes).build();
    }

    @Override
    public CommandProcessingResult approveDocument(final Long documentId, final JsonCommand command) {
        final KycDocument document = this.repositoryWrapper.findOneWithNotFoundDetection(documentId);

        final Map<String, Object> changes = document.approve();
        if (!changes.isEmpty()) {
            this.repository.saveAndFlush(document);
            this.evaluationService.evaluateKycStatus(document.getClientId());
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(document.getClientId())
                .withEntityId(documentId).with(changes).build();
    }

    @Override
    public CommandProcessingResult rejectDocument(final Long documentId, final JsonCommand command) {
        final KycDocument document = this.repositoryWrapper.findOneWithNotFoundDetection(documentId);

        final Map<String, Object> changes = document.reject();
        if (!changes.isEmpty()) {
            this.repository.saveAndFlush(document);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(document.getClientId())
                .withEntityId(documentId).with(changes).build();
    }
}
