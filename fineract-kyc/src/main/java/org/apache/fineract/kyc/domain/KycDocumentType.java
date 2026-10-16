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
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.kyc.api.KycApiConstants;

@Entity
@Getter
@Table(name = "m_kyc_document_type")
public class KycDocumentType extends AbstractAuditableCustom {

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected KycDocumentType() {}

    private KycDocumentType(final String code, final String name, final String description, final boolean active) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public static KycDocumentType create(final String code, final String name, final String description, final boolean active) {
        return new KycDocumentType(code, name, description, active);
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

        if (command.isChangeInBooleanParameterNamed(KycApiConstants.activeParamName, this.active)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(KycApiConstants.activeParamName);
            changes.put(KycApiConstants.activeParamName, newValue);
            this.active = newValue;
        }

        return changes;
    }
}
