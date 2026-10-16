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
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;

@Entity
@Getter
@Table(name = "m_kyc_case_flag")
public class KycCaseFlag extends AbstractAuditableCustom {

    @ManyToOne
    @JoinColumn(name = "kyc_case_id", nullable = false)
    private KycCase kycCase;

    @ManyToOne
    @JoinColumn(name = "kyc_flag_type_id", nullable = false)
    private KycFlagType flagType;

    @Column(name = "status_enum", nullable = false)
    private Integer status;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "resolution_remarks", length = 1000)
    private String resolutionRemarks;

    protected KycCaseFlag() {}

    private KycCaseFlag(final KycCase kycCase, final KycFlagType flagType, final String remarks) {
        this.kycCase = kycCase;
        this.flagType = flagType;
        this.status = KycCaseFlagStatus.ACTIVE.getValue();
        this.remarks = remarks;
    }

    public static KycCaseFlag create(final KycCase kycCase, final KycFlagType flagType, final String remarks) {
        return new KycCaseFlag(kycCase, flagType, remarks);
    }

    public void resolve(final String resolutionRemarks) {
        this.status = KycCaseFlagStatus.RESOLVED.getValue();
        this.resolutionRemarks = resolutionRemarks;
    }

    public void markAsFalsePositive(final String resolutionRemarks) {
        this.status = KycCaseFlagStatus.FALSE_POSITIVE.getValue();
        this.resolutionRemarks = resolutionRemarks;
    }

    public boolean isActive() {
        return KycCaseFlagStatus.fromInt(this.status).equals(KycCaseFlagStatus.ACTIVE);
    }
}
