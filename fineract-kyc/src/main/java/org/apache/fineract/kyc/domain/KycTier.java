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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.kyc.api.KycApiConstants;

@Entity
@Getter
@Table(name = "m_kyc_tier")
public class KycTier extends AbstractAuditableCustom {

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "max_balance", scale = 6, precision = 19)
    private BigDecimal maxBalance;

    @Column(name = "max_transaction_amount", scale = 6, precision = 19)
    private BigDecimal maxTransactionAmount;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "client_type_cv_id")
    private Long clientTypeCvId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "kyctier")
    private Set<KycTierDocumentRequirement> documentRequirements = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "kyctier")
    private Set<KycTierRequiredField> requiredFields = new HashSet<>();

    protected KycTier() {}

    private KycTier(final String code, final String name, final String description, final Integer level, final BigDecimal maxBalance,
            final BigDecimal maxTransactionAmount, final boolean active, final Integer validityDays, final Long clientTypeCvId) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.level = level;
        this.maxBalance = maxBalance;
        this.maxTransactionAmount = maxTransactionAmount;
        this.active = active;
        this.validityDays = validityDays;
        this.clientTypeCvId = clientTypeCvId;
    }

    public static KycTier create(final String code, final String name, final String description, final Integer level,
            final BigDecimal maxBalance, final BigDecimal maxTransactionAmount, final boolean active, final Integer validityDays,
            final Long clientTypeCvId) {
        return new KycTier(code, name, description, level, maxBalance, maxTransactionAmount, active, validityDays, clientTypeCvId);
    }

    public Map<String, Object> update(final JsonCommand command) {
        final Map<String, Object> changes = new HashMap<>();

        if (command.isChangeInStringParameterNamed(KycApiConstants.nameParamName, this.name)) {
            final String newValue = command.stringValueOfParameterNamed(KycApiConstants.nameParamName);
            changes.put(KycApiConstants.nameParamName, newValue);
            this.name = StringUtils.defaultIfEmpty(newValue, null);
        }

        if (command.isChangeInStringParameterNamed(KycApiConstants.descriptionParamName, this.description)) {
            final String newValue = command.stringValueOfParameterNamed(KycApiConstants.descriptionParamName);
            changes.put(KycApiConstants.descriptionParamName, newValue);
            this.description = StringUtils.defaultIfEmpty(newValue, null);
        }

        if (command.isChangeInIntegerParameterNamed(KycApiConstants.levelParamName, this.level)) {
            final Integer newValue = command.integerValueOfParameterNamed(KycApiConstants.levelParamName);
            changes.put(KycApiConstants.levelParamName, newValue);
            this.level = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamed(KycApiConstants.maxBalanceParamName, this.maxBalance)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(KycApiConstants.maxBalanceParamName);
            changes.put(KycApiConstants.maxBalanceParamName, newValue);
            this.maxBalance = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamed(KycApiConstants.maxTransactionAmountParamName, this.maxTransactionAmount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(KycApiConstants.maxTransactionAmountParamName);
            changes.put(KycApiConstants.maxTransactionAmountParamName, newValue);
            this.maxTransactionAmount = newValue;
        }

        if (command.isChangeInBooleanParameterNamed(KycApiConstants.activeParamName, this.active)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(KycApiConstants.activeParamName);
            changes.put(KycApiConstants.activeParamName, newValue);
            this.active = newValue;
        }

        if (command.isChangeInIntegerParameterNamed("validityDays", this.validityDays)) {
            final Integer newValue = command.integerValueOfParameterNamed("validityDays");
            changes.put("validityDays", newValue);
            this.validityDays = newValue;
        }

        if (command.isChangeInLongParameterNamed("clientTypeCvId", this.clientTypeCvId)) {
            final Long newValue = command.longValueOfParameterNamed("clientTypeCvId");
            changes.put("clientTypeCvId", newValue);
            this.clientTypeCvId = newValue;
        }

        return changes;
    }

    public void updateDocumentRequirements(final Set<KycTierDocumentRequirement> newRequirements) {
        this.documentRequirements.clear();
        this.documentRequirements.addAll(newRequirements);
    }

    public void updateRequiredFields(final Set<KycTierRequiredField> newFields) {
        this.requiredFields.clear();
        this.requiredFields.addAll(newFields);
    }
}
