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
import java.time.LocalDateTime;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

/**
 * Stores workflow state transitions only (separated from audit). Maps to m_kyc_case_history.
 */
@Entity
@Getter
@Table(name = "m_kyc_case_history")
public class KycCaseHistory extends AbstractPersistableCustom<Long> {

    @ManyToOne
    @JoinColumn(name = "kyc_case_id", nullable = false)
    private KycCase kycCase;

    @Column(name = "from_status_enum")
    private Integer fromStatus;

    @Column(name = "to_status_enum", nullable = false)
    private Integer toStatus;

    @Column(name = "from_assignment_status_enum")
    private Integer fromAssignmentStatus;

    @Column(name = "to_assignment_status_enum")
    private Integer toAssignmentStatus;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected KycCaseHistory() {}

    public static KycCaseHistory createWorkflowTransition(final KycCase kycCase, final Integer fromStatus, final Integer toStatus,
            final String comments, final Long actorId) {
        final KycCaseHistory history = new KycCaseHistory();
        history.kycCase = kycCase;
        history.fromStatus = fromStatus;
        history.toStatus = toStatus;
        history.comments = comments;
        history.actorId = actorId;
        history.createdDate = LocalDateTime.now();
        return history;
    }

    public static KycCaseHistory createAssignmentTransition(final KycCase kycCase, final Integer fromAssignmentStatus,
            final Integer toAssignmentStatus, final String comments, final Long actorId) {
        final KycCaseHistory history = new KycCaseHistory();
        history.kycCase = kycCase;
        history.fromAssignmentStatus = fromAssignmentStatus;
        history.toAssignmentStatus = toAssignmentStatus;
        // Keep track of the current workflow status during assignment to satisfy the DB constraint
        history.fromStatus = kycCase.getWorkflowStatus();
        history.toStatus = kycCase.getWorkflowStatus();
        history.comments = comments;
        history.actorId = actorId;
        history.createdDate = LocalDateTime.now();
        return history;
    }
}
