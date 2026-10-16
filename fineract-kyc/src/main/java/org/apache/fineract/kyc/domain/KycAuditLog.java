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
 * Stores field-level change audit (separated from workflow history). Maps to m_kyc_audit_log.
 */
@Entity
@Getter
@Table(name = "m_kyc_audit_log")
public class KycAuditLog extends AbstractPersistableCustom<Long> {

    @ManyToOne
    @JoinColumn(name = "kyc_case_id", nullable = false)
    private KycCase kycCase;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected KycAuditLog() {}

    public static KycAuditLog create(final KycCase kycCase, final String entityType, final Long entityId, final String action,
            final String fieldName, final String oldValue, final String newValue, final String comments, final Long actorId,
            final String ipAddress) {
        final KycAuditLog log = new KycAuditLog();
        log.kycCase = kycCase;
        log.entityType = entityType;
        log.entityId = entityId;
        log.action = action;
        log.fieldName = fieldName;
        log.oldValue = oldValue;
        log.newValue = newValue;
        log.comments = comments;
        log.actorId = actorId;
        log.ipAddress = ipAddress;
        log.createdDate = LocalDateTime.now();
        return log;
    }
}
