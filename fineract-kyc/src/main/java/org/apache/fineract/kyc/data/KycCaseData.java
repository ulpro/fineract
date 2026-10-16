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
package org.apache.fineract.kyc.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycCaseData {

    private final Long id;
    private final Long clientId;
    private final String clientName;

    // Tier info
    private final KycTierData tier;

    // Status
    private final Integer workflowStatusId;
    private final String workflowStatusCode;
    private final String workflowStatusValue;

    // Assignment
    private final Long assignedAgentId;
    private final String assignedAgentName;
    private final LocalDateTime assignedDate;
    private final Integer assignmentStatusId;
    private final String assignmentStatusCode;
    private final String assignmentStatusValue;

    // Risk
    private final Integer riskLevelId;
    private final String riskLevelCode;
    private final String riskLevelValue;
    private final BigDecimal riskScore;
    private final String riskReason;
    private final List<KycCaseFlagData> flags;

    // Metadata
    private final String escalationReason;
    private final String complianceNotes;
    private final LocalDate nextReviewDate;

    // Documents and Completion
    private final List<KycDocumentData> documents;
    private final List<KycTierDocumentRequirementData> missingRequirements;
    private final Integer requiredDocumentCount;
    private final Integer submittedDocumentCount;
    private final Integer completionPercentage;

    // Fineract Audit
    private final LocalDateTime createdDate;
    private final String createdByUsername;
    private final LocalDateTime lastModifiedDate;
    private final String lastModifiedByUsername;
}
