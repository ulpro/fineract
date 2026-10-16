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
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Getter
@Table(name = "m_kyc_tier_required_field")
public class KycTierRequiredField extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "kyc_tier_id", nullable = false)
    private KycTier kyctier;

    @Column(name = "field_name", length = 100, nullable = false)
    private String fieldName;

    protected KycTierRequiredField() {}

    private KycTierRequiredField(final KycTier tier, final String fieldName) {
        this.kyctier = tier;
        this.fieldName = fieldName;
    }

    public static KycTierRequiredField create(final KycTier tier, final String fieldName) {
        return new KycTierRequiredField(tier, fieldName);
    }

    public void updateTier(final KycTier tier) {
        this.kyctier = tier;
    }
}
