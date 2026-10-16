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
package org.apache.fineract.kyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.kyc.api.KycApiConstants;

@Entity
@Getter
@Table(name = "m_kyc_document")
public class KycDocument extends AbstractAuditableCustom {

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @ManyToOne
    @JoinColumn(name = "kyc_document_type_id", nullable = false)
    private KycDocumentType documentType;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "file_location", length = 500)
    private String fileLocation;

    @Column(name = "verification_status_enum", nullable = false)
    private Integer verificationStatus;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    protected KycDocument() {}

    private KycDocument(final Long clientId, final KycDocumentType documentType, final String documentNumber, final LocalDate issueDate,
            final LocalDate expiryDate, final String fileLocation, final KycDocumentStatus status, final String remarks) {
        this.clientId = clientId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.fileLocation = fileLocation;
        this.verificationStatus = status != null ? status.getValue() : KycDocumentStatus.PENDING.getValue();
        this.remarks = remarks;
    }

    public static KycDocument create(final Long clientId, final KycDocumentType documentType, final String documentNumber,
            final LocalDate issueDate, final LocalDate expiryDate, final String fileLocation, final KycDocumentStatus status,
            final String remarks) {
        return new KycDocument(clientId, documentType, documentNumber, issueDate, expiryDate, fileLocation, status, remarks);
    }

    public Map<String, Object> update(final JsonCommand command, final KycDocumentType documentType) {
        final Map<String, Object> changes = new HashMap<>();

        if (documentType != null && !documentType.getId().equals(this.documentType.getId())) {
            changes.put(KycApiConstants.documentTypeIdParamName, documentType.getId());
            this.documentType = documentType;
        }

        if (command.isChangeInStringParameterNamed(KycApiConstants.documentNumberParamName, this.documentNumber)) {
            final String newValue = command.stringValueOfParameterNamed(KycApiConstants.documentNumberParamName);
            changes.put(KycApiConstants.documentNumberParamName, newValue);
            this.documentNumber = StringUtils.defaultIfEmpty(newValue, null);
        }

        if (command.isChangeInLocalDateParameterNamed(KycApiConstants.issueDateParamName, this.issueDate)) {
            final LocalDate newValue = command.localDateValueOfParameterNamed(KycApiConstants.issueDateParamName);
            changes.put(KycApiConstants.issueDateParamName, newValue);
            this.issueDate = newValue;
        }

        if (command.isChangeInLocalDateParameterNamed(KycApiConstants.expiryDateParamName, this.expiryDate)) {
            final LocalDate newValue = command.localDateValueOfParameterNamed(KycApiConstants.expiryDateParamName);
            changes.put(KycApiConstants.expiryDateParamName, newValue);
            this.expiryDate = newValue;
        }

        if (command.isChangeInStringParameterNamed(KycApiConstants.fileLocationParamName, this.fileLocation)) {
            final String newValue = command.stringValueOfParameterNamed(KycApiConstants.fileLocationParamName);
            changes.put(KycApiConstants.fileLocationParamName, newValue);
            this.fileLocation = StringUtils.defaultIfEmpty(newValue, null);
        }

        if (command.isChangeInIntegerParameterNamed(KycApiConstants.verificationStatusParamName, this.verificationStatus)) {
            final Integer newValue = command.integerValueOfParameterNamed(KycApiConstants.verificationStatusParamName);
            changes.put(KycApiConstants.verificationStatusParamName, newValue);
            this.verificationStatus = KycDocumentStatus.fromInt(newValue).getValue();
        }

        if (command.isChangeInStringParameterNamed(KycApiConstants.remarksParamName, this.remarks)) {
            final String newValue = command.stringValueOfParameterNamed(KycApiConstants.remarksParamName);
            changes.put(KycApiConstants.remarksParamName, newValue);
            this.remarks = StringUtils.defaultIfEmpty(newValue, null);
        }

        return changes;
    }

    public Map<String, Object> submit() {
        final Map<String, Object> changes = new HashMap<>();
        if (!KycDocumentStatus.fromInt(this.verificationStatus).equals(KycDocumentStatus.PENDING)) {
            throw new GeneralPlatformDomainRuleException("error.msg.kycdocument.submit.invalid.status",
                    "Document cannot be submitted for review unless it is in pending state", this.getId());
        }

        final KycDocumentStatus newStatus = KycDocumentStatus.UNDER_REVIEW;
        this.verificationStatus = newStatus.getValue();
        changes.put(KycApiConstants.verificationStatusParamName, newStatus.getValue());
        return changes;
    }

    public Map<String, Object> approve() {
        final Map<String, Object> changes = new HashMap<>();
        if (!KycDocumentStatus.fromInt(this.verificationStatus).equals(KycDocumentStatus.UNDER_REVIEW)) {
            throw new GeneralPlatformDomainRuleException("error.msg.kycdocument.approve.invalid.status",
                    "Document cannot be approved unless it is under review", this.getId());
        }

        final KycDocumentStatus newStatus = KycDocumentStatus.VERIFIED;
        this.verificationStatus = newStatus.getValue();
        changes.put(KycApiConstants.verificationStatusParamName, newStatus.getValue());
        return changes;
    }

    public Map<String, Object> reject() {
        final Map<String, Object> changes = new HashMap<>();
        if (!KycDocumentStatus.fromInt(this.verificationStatus).equals(KycDocumentStatus.UNDER_REVIEW)) {
            throw new GeneralPlatformDomainRuleException("error.msg.kycdocument.reject.invalid.status",
                    "Document cannot be rejected unless it is under review", this.getId());
        }

        final KycDocumentStatus newStatus = KycDocumentStatus.REJECTED;
        this.verificationStatus = newStatus.getValue();
        changes.put(KycApiConstants.verificationStatusParamName, newStatus.getValue());
        return changes;
    }
}
