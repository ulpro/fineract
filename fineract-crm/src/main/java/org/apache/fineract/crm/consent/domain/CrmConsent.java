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
package org.apache.fineract.crm.consent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "crm_consent")
@Getter
@Setter
@NoArgsConstructor
public class CrmConsent extends AbstractPersistableCustom<Long> {

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "prospect_id")
    private Long prospectId;

    @Column(name = "channel_enum", nullable = false)
    private Integer channelEnum;

    @Column(name = "purpose", nullable = false, length = 100)
    private String purpose;

    @Column(name = "is_granted", nullable = false)
    private Boolean isGranted;

    @Column(name = "granted_date")
    private LocalDateTime grantedDate;

    @Column(name = "revoked_date")
    private LocalDateTime revokedDate;

    @Column(name = "proof")
    private String proof;

    public CrmConsent(Long clientId, Long prospectId, Integer channelEnum, String purpose, Boolean isGranted, String proof) {
        this.clientId = clientId;
        this.prospectId = prospectId;
        this.channelEnum = channelEnum;
        this.purpose = purpose;
        this.isGranted = isGranted;
        this.grantedDate = isGranted ? LocalDateTime.now() : null;
        this.revokedDate = !isGranted ? LocalDateTime.now() : null;
        this.proof = proof;
    }
}
