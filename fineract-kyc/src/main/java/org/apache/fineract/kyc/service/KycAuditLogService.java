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
package org.apache.fineract.kyc.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.kyc.domain.KycAuditLog;
import org.apache.fineract.kyc.domain.KycAuditLogRepository;
import org.apache.fineract.kyc.domain.KycCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Transactional
@RequiredArgsConstructor
public class KycAuditLogService {

    private final KycAuditLogRepository repository;
    private final PlatformSecurityContext context;

    public void logWorkflowTransition(final KycCase kycCase, final Integer oldStatus, final Integer newStatus, final String comments) {
        saveAuditLog(kycCase, "CASE", kycCase.getId(), "TRANSITION", "workflowStatus", String.valueOf(oldStatus), String.valueOf(newStatus),
                comments);
    }

    public void logAssignment(final KycCase kycCase, final Long oldAgentId, final Long newAgentId, final String comments) {
        saveAuditLog(kycCase, "CASE", kycCase.getId(), "ASSIGN", "assignedAgentId", String.valueOf(oldAgentId), String.valueOf(newAgentId),
                comments);
    }

    public void logRiskUpdate(final KycCase kycCase, final Map<String, Object> changes, final String comments) {
        if (changes.containsKey("riskLevel_new")) {
            saveAuditLog(kycCase, "CASE", kycCase.getId(), "UPDATERISK", "riskLevel", String.valueOf(changes.get("riskLevel_old")),
                    String.valueOf(changes.get("riskLevel_new")), comments);
        }
        if (changes.containsKey("riskScore_new")) {
            saveAuditLog(kycCase, "CASE", kycCase.getId(), "UPDATERISK", "riskScore", String.valueOf(changes.get("riskScore_old")),
                    String.valueOf(changes.get("riskScore_new")), comments);
        }
        if (changes.containsKey("riskReason_new")) {
            saveAuditLog(kycCase, "CASE", kycCase.getId(), "UPDATERISK", "riskReason", String.valueOf(changes.get("riskReason_old")),
                    String.valueOf(changes.get("riskReason_new")), comments);
        }
        if (changes.containsKey("riskFlags_new")) {
            saveAuditLog(kycCase, "CASE", kycCase.getId(), "UPDATERISK", "riskFlags", String.valueOf(changes.get("riskFlags_old")),
                    String.valueOf(changes.get("riskFlags_new")), comments);
        }
    }

    public void logDocumentAction(final KycCase kycCase, final Long documentId, final String action, final String oldStatus,
            final String newStatus, final String comments) {
        saveAuditLog(kycCase, "DOCUMENT", documentId, action, "verificationStatus", oldStatus, newStatus, comments);
    }

    private void saveAuditLog(final KycCase kycCase, final String entityType, final Long entityId, final String action,
            final String fieldName, final String oldValue, final String newValue, final String comments) {

        final Long actorId = context.authenticatedUser().getId();
        final String ipAddress = determineIpAddress();

        final KycAuditLog log = KycAuditLog.create(kycCase, entityType, entityId, action, fieldName, oldValue, newValue, comments, actorId,
                ipAddress);
        repository.save(log);
    }

    private String determineIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}
