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
package org.apache.fineract.crm.interaction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "crm_interaction")
@Getter
@Setter
@NoArgsConstructor
public class CrmInteraction extends AbstractPersistableCustom<Long> {

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "channel_cv_id")
    private Long channelCodeValueId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "notes")
    private String notes;

    @Column(name = "interaction_date")
    private LocalDateTime interactionDate;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    public CrmInteraction(Long clientId, Long channelCodeValueId, Long staffId, String subject,
            String notes, LocalDateTime interactionDate, BigDecimal latitude, BigDecimal longitude) {
        this.clientId = clientId;
        this.channelCodeValueId = channelCodeValueId;
        this.staffId = staffId;
        this.subject = subject;
        this.notes = notes;
        this.interactionDate = interactionDate != null ? interactionDate : LocalDateTime.now();
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
