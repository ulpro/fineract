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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "crm_task")
@Getter
@Setter
@NoArgsConstructor
public class CrmTask extends AbstractPersistableCustom<Long> {

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "type_enum", nullable = false)
    private Integer typeEnum; // 0=KYC_REVIEW, 1=OVERDUE_RECALL, 2=VIP_CHECK, 3=PROSPECT_FOLLOWUP, 4=MANUAL

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "description")
    private String description;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "priority_enum")
    private Integer priorityEnum;

    @Column(name = "status_enum", nullable = false)
    private Integer statusEnum; // 0=OPEN, 1=IN_PROGRESS, 2=COMPLETED, 3=CANCELLED

    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    public CrmTask(Long clientId, Integer typeEnum, String subject, String description,
            LocalDateTime dueDate, Integer priorityEnum, Integer statusEnum, Long assignedToUserId) {
        this.clientId = clientId;
        this.typeEnum = typeEnum;
        this.subject = subject;
        this.description = description;
        this.dueDate = dueDate;
        this.priorityEnum = priorityEnum != null ? priorityEnum : 1;
        this.statusEnum = statusEnum != null ? statusEnum : 0;
        this.assignedToUserId = assignedToUserId;
    }
}
