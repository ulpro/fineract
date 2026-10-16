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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;

@Entity
@Getter
@Table(name = "m_kyc_case")
public class KycCase extends AbstractAuditableCustom {

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @ManyToOne
    @JoinColumn(name = "kyc_tier_id")
    private KycTier tier;

    @Column(name = "workflow_status_enum", nullable = false)
    private Integer workflowStatus;

    // Assignment Ownership
    @Column(name = "assigned_agent_id")
    private Long assignedAgentId;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @Column(name = "assignment_status_enum", nullable = false)
    private Integer assignmentStatus;

    // Risk Model
    @Column(name = "risk_level_enum")
    private Integer riskLevel;

    @Column(name = "risk_score", scale = 2, precision = 5)
    private BigDecimal riskScore;

    @Column(name = "risk_reason", length = 1000)
    private String riskReason;

    // Case metadata
    @Column(name = "escalation_reason", length = 500)
    private String escalationReason;

    @Column(name = "compliance_notes", columnDefinition = "TEXT")
    private String complianceNotes;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    protected KycCase() {}

    private KycCase(final Long clientId, final KycTier tier) {
        this.clientId = clientId;
        this.tier = tier;
        this.workflowStatus = KycCaseWorkflowStatus.EN_ATTENTE.getValue();
        this.assignmentStatus = KycAssignmentStatus.UNASSIGNED.getValue();
        this.riskLevel = KycRiskLevel.LOW.getValue();
    }

    public static KycCase create(final Long clientId, final KycTier tier) {
        return new KycCase(clientId, tier);
    }

    public KycCaseWorkflowStatus getWorkflowStatusEnum() {
        return KycCaseWorkflowStatus.fromInt(this.workflowStatus);
    }

    public KycAssignmentStatus getAssignmentStatusEnum() {
        return KycAssignmentStatus.fromInt(this.assignmentStatus);
    }

    public KycRiskLevel getRiskLevelEnum() {
        return KycRiskLevel.fromInt(this.riskLevel);
    }

    public Map<String, Object> transitionWorkflow(final KycCaseWorkflowStatus newStatus) {
        final Map<String, Object> changes = new HashMap<>();
        final KycCaseWorkflowStatus currentStatus = getWorkflowStatusEnum();

        validateWorkflowTransition(currentStatus, newStatus);

        changes.put("fromStatus", currentStatus.getValue());
        changes.put("toStatus", newStatus.getValue());
        this.workflowStatus = newStatus.getValue();
        return changes;
    }

    public Map<String, Object> assign(final Long agentId) {
        final Map<String, Object> changes = new HashMap<>();
        changes.put("assignedAgentId", agentId);
        changes.put("fromAssignmentStatus", this.assignmentStatus);
        this.assignedAgentId = agentId;
        this.assignedDate = LocalDateTime.now();
        this.assignmentStatus = KycAssignmentStatus.ASSIGNED.getValue();
        changes.put("toAssignmentStatus", this.assignmentStatus);
        return changes;
    }

    public Map<String, Object> updateAssignmentStatus(final KycAssignmentStatus newStatus) {
        final Map<String, Object> changes = new HashMap<>();
        changes.put("fromAssignmentStatus", this.assignmentStatus);
        this.assignmentStatus = newStatus.getValue();
        changes.put("toAssignmentStatus", this.assignmentStatus);
        return changes;
    }

    public Map<String, Object> updateRisk(final Integer riskLevelValue, final BigDecimal score, final String reason) {
        final Map<String, Object> changes = new HashMap<>();

        if (riskLevelValue != null) {
            changes.put("riskLevel_old", this.riskLevel);
            this.riskLevel = KycRiskLevel.fromInt(riskLevelValue).getValue();
            changes.put("riskLevel_new", this.riskLevel);
        }
        if (score != null) {
            changes.put("riskScore_old", this.riskScore);
            this.riskScore = score;
            changes.put("riskScore_new", this.riskScore);
        }
        if (reason != null) {
            changes.put("riskReason_old", this.riskReason);
            this.riskReason = reason;
            changes.put("riskReason_new", this.riskReason);
        }
        return changes;
    }

    public void updateTier(final KycTier newTier) {
        this.tier = newTier;
    }

    public void updateEscalationReason(final String reason) {
        this.escalationReason = reason;
    }

    public void updateComplianceNotes(final String notes) {
        this.complianceNotes = notes;
    }

    public void updateNextReviewDate(final LocalDate date) {
        this.nextReviewDate = date;
    }

    private void validateWorkflowTransition(final KycCaseWorkflowStatus from, final KycCaseWorkflowStatus to) {
        // Validation logic delegated to KycEvaluationService and workflow rules
    }
}
