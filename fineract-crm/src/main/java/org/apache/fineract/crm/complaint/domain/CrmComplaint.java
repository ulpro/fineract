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
package org.apache.fineract.crm.complaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "crm_complaint")
@Getter
@Setter
@NoArgsConstructor
public class CrmComplaint extends AbstractPersistableCustom<Long> {

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "channel_cv_id")
    private Long channelCodeValueId;

    @Column(name = "category_cv_id")
    private Long categoryCodeValueId;

    @Column(name = "priority_enum", nullable = false)
    private Integer priorityEnum; // 0=LOW, 1=NORMAL, 2=HIGH, 3=CRITICAL

    @Column(name = "status_enum", nullable = false)
    private Integer statusEnum; // 0=REGISTERED, 1=ACKNOWLEDGED, 2=ANALYSIS, 3=PENDING_CLIENT, 4=RESOLVED, 5=CLOSED, 6=ESCALATED, 7=REJECTED

    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    @Column(name = "office_id")
    private Long officeId;

    @Column(name = "description")
    private String description;

    @Column(name = "resolution_summary")
    private String resolutionSummary;

    @Column(name = "satisfaction_score")
    private Integer satisfactionScore;

    @Column(name = "is_fraud", nullable = false)
    private Boolean isFraud;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "target_resolution_date")
    private LocalDateTime targetResolutionDate;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    public CrmComplaint(String ticketNumber, Long clientId, Long accountId, Long transactionId,
            Long channelCodeValueId, Long categoryCodeValueId, Integer priorityEnum,
            Integer statusEnum, Long assignedToUserId, Long officeId, String description,
            Boolean isFraud, LocalDateTime targetResolutionDate) {
        this.ticketNumber = ticketNumber;
        this.clientId = clientId;
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.channelCodeValueId = channelCodeValueId;
        this.categoryCodeValueId = categoryCodeValueId;
        this.priorityEnum = priorityEnum;
        this.statusEnum = statusEnum;
        this.assignedToUserId = assignedToUserId;
        this.officeId = officeId;
        this.description = description;
        this.isFraud = isFraud != null ? isFraud : false;
        this.createdDate = LocalDateTime.now();
        this.targetResolutionDate = targetResolutionDate;
    }
}
