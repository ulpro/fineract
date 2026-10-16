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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.kyc.data.KycAuditLogData;
import org.apache.fineract.kyc.data.KycCaseData;
import org.apache.fineract.kyc.data.KycCaseFlagData;
import org.apache.fineract.kyc.data.KycCaseHistoryData;
import org.apache.fineract.kyc.data.KycDocumentData;
import org.apache.fineract.kyc.data.KycFlagTypeData;
import org.apache.fineract.kyc.data.KycTierData;
import org.apache.fineract.kyc.domain.KycAssignmentStatus;
import org.apache.fineract.kyc.domain.KycCaseWorkflowStatus;
import org.apache.fineract.kyc.domain.KycRiskLevel;
import org.apache.fineract.kyc.exception.KycCaseNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KycCaseReadPlatformServiceImpl implements KycCaseReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PaginationHelper paginationHelper;
    private final KycDocumentReadPlatformService documentReadPlatformService;
    private final KycTierReadPlatformService tierReadPlatformService;

    private final KycCaseMapper caseMapper = new KycCaseMapper();
    private final KycCaseHistoryMapper historyMapper = new KycCaseHistoryMapper();
    private final KycAuditLogMapper auditLogMapper = new KycAuditLogMapper();

    @Override
    public Page<KycCaseData> retrieveAllCases(final SearchParameters searchParameters, final Long tierId, final Integer statusId,
            final Integer riskLevelId, final Integer assignmentStatusId) {

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select ").append(sqlGenerator.calcFoundRows()).append(" ");
        sqlBuilder.append(this.caseMapper.schema());

        List<Object> paramList = new ArrayList<>();
        String whereOrAnd = " where ";

        if (tierId != null) {
            sqlBuilder.append(whereOrAnd).append(" c.kyc_tier_id = ? ");
            paramList.add(tierId);
            whereOrAnd = " and ";
        }
        if (statusId != null) {
            sqlBuilder.append(whereOrAnd).append(" c.workflow_status_enum = ? ");
            paramList.add(statusId);
            whereOrAnd = " and ";
        }
        if (riskLevelId != null) {
            sqlBuilder.append(whereOrAnd).append(" c.risk_level_enum = ? ");
            paramList.add(riskLevelId);
            whereOrAnd = " and ";
        }
        if (assignmentStatusId != null) {
            sqlBuilder.append(whereOrAnd).append(" c.assignment_status_enum = ? ");
            paramList.add(assignmentStatusId);
            whereOrAnd = " and ";
        }
        if (searchParameters.getName() != null) {
            sqlBuilder.append(whereOrAnd).append(" (c.risk_reason like ? or c.compliance_notes like ? or cl.display_name like ?) ");
            final String searchSql = "%" + searchParameters.getName() + "%";
            paramList.add(searchSql);
            paramList.add(searchSql);
            paramList.add(searchSql);
        }

        if (searchParameters.hasOrderBy()) {
            sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());
            if (searchParameters.hasSortOrder()) {
                sqlBuilder.append(' ').append(searchParameters.getSortOrder());
            }
        } else {
            sqlBuilder.append(" order by c.id desc ");
        }

        if (searchParameters.hasLimit()) {
            sqlBuilder.append(" ");
            if (searchParameters.hasOffset()) {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
            } else {
                sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
            }
        }

        Page<KycCaseData> page = this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), paramList.toArray(),
                this.caseMapper);
        List<KycCaseData> enrichedItems = new ArrayList<>();
        for (KycCaseData item : page.getPageItems()) {
            enrichedItems.add(enrichWithDocumentsAndTier(enrichWithFlags(item)));
        }
        return new Page<>(enrichedItems, page.getTotalFilteredRecords());
    }

    @Override
    public KycCaseData retrieveOne(final Long caseId) {
        final String sql = "select " + this.caseMapper.schema() + " where c.id = ?";
        final KycCaseData caseData = this.jdbcTemplate.queryForObject(sql, this.caseMapper, caseId);

        if (caseData == null) {
            throw new KycCaseNotFoundException(caseId);
        }

        // Enrich with tier requirements and documents
        return enrichWithDocumentsAndTier(enrichWithFlags(caseData));
    }

    @Override
    public List<KycCaseData> retrieveAllForClient(final Long clientId) {
        final String sql = "select " + this.caseMapper.schema() + " where c.client_id = ?";
        return this.jdbcTemplate.query(sql, this.caseMapper, clientId);
    }

    @Override
    public List<KycCaseHistoryData> retrieveHistory(final Long caseId) {
        final String sql = "select " + this.historyMapper.schema() + " where h.kyc_case_id = ? order by h.created_date asc";
        return this.jdbcTemplate.query(sql, this.historyMapper, caseId);
    }

    @Override
    public List<KycAuditLogData> retrieveAuditLogs(final Long caseId) {
        final String sql = "select " + this.auditLogMapper.schema() + " where a.kyc_case_id = ? order by a.created_date desc";
        return this.jdbcTemplate.query(sql, this.auditLogMapper, caseId);
    }

    private KycCaseData enrichWithDocumentsAndTier(final KycCaseData caseData) {
        // We need to fetch the full Tier to get the document requirements
        KycTierData tierData = null;
        if (caseData.getTier() != null && caseData.getTier().getId() != null) {
            try {
                tierData = this.tierReadPlatformService.retrieveOne(caseData.getTier().getId());
            } catch (Exception e) {
                // Ignore
            }
        }

        // Fetch submitted documents for the client
        List<KycDocumentData> allDocs = this.documentReadPlatformService.retrieveAllByClientId(caseData.getClientId());

        // Filter out rejected (400) and expired (500) documents
        long activeCount = allDocs.stream().filter(d -> d.getVerificationStatus() == null
                || (d.getVerificationStatus().getId() != 400L && d.getVerificationStatus().getId() != 500L)).count();

        // Calculate missing docs and completion percentage
        int requiredCount = 0;
        int submittedCount = (int) activeCount;
        int completion = 0;

        if (tierData != null && tierData.getDocumentRequirements() != null) {
            requiredCount = tierData.getDocumentRequirements().size();
            if (requiredCount > 0) {
                // Logic to see which required docs match submitted docs by type
                long matchedDocs = tierData.getDocumentRequirements().stream()
                        .filter(req -> allDocs.stream().anyMatch(
                                doc -> doc.getDocumentTypeId().equals(req.getDocumentTypeId()) && (doc.getVerificationStatus() == null
                                        || (doc.getVerificationStatus().getId() != 400L && doc.getVerificationStatus().getId() != 500L))))
                        .count();
                completion = (int) ((matchedDocs * 100) / requiredCount);
            }
        }

        return KycCaseData.builder().id(caseData.getId()).clientId(caseData.getClientId()).clientName(caseData.getClientName())
                .tier(tierData != null ? tierData : caseData.getTier()).workflowStatusId(caseData.getWorkflowStatusId())
                .workflowStatusCode(caseData.getWorkflowStatusCode()).workflowStatusValue(caseData.getWorkflowStatusValue())
                .assignedAgentId(caseData.getAssignedAgentId()).assignedAgentName(caseData.getAssignedAgentName())
                .assignedDate(caseData.getAssignedDate()).assignmentStatusId(caseData.getAssignmentStatusId())
                .assignmentStatusCode(caseData.getAssignmentStatusCode()).assignmentStatusValue(caseData.getAssignmentStatusValue())
                .riskLevelId(caseData.getRiskLevelId()).riskLevelCode(caseData.getRiskLevelCode())
                .riskLevelValue(caseData.getRiskLevelValue()).riskScore(caseData.getRiskScore()).riskReason(caseData.getRiskReason())
                .flags(caseData.getFlags()).escalationReason(caseData.getEscalationReason()).complianceNotes(caseData.getComplianceNotes())
                .documents(allDocs).requiredDocumentCount(requiredCount).submittedDocumentCount(submittedCount)
                .completionPercentage(completion).createdDate(caseData.getCreatedDate()).createdByUsername(caseData.getCreatedByUsername())
                .lastModifiedDate(caseData.getLastModifiedDate()).lastModifiedByUsername(caseData.getLastModifiedByUsername()).build();
    }

    private KycCaseData enrichWithFlags(final KycCaseData caseData) {
        final String sql = "SELECT f.id, f.kyc_case_id as kycCaseId, f.kyc_flag_type_id as flagTypeId, "
                + "ft.code as flagTypeCode, ft.name as flagTypeName, ft.description as flagTypeDescription, ft.override_status_enum as overrideStatus, "
                + "f.status_enum as statusId, f.remarks, f.resolution_remarks as resolutionRemarks, f.created_date as createdDate "
                + "FROM m_kyc_case_flag f JOIN m_kyc_flag_type ft ON ft.id = f.kyc_flag_type_id " + "WHERE f.kyc_case_id = ?";
        List<KycCaseFlagData> flags = this.jdbcTemplate.query(sql, (rs, rowNum) -> {
            KycFlagTypeData type = KycFlagTypeData.builder().id(rs.getLong("flagTypeId")).code(rs.getString("flagTypeCode"))
                    .name(rs.getString("flagTypeName")).description(rs.getString("flagTypeDescription"))
                    .overrideStatus(new EnumOptionData(rs.getLong("overrideStatus"), null, null)).build();
            return KycCaseFlagData.builder().id(rs.getLong("id")).kycCaseId(rs.getLong("kycCaseId")).flagType(type)
                    .status(new EnumOptionData(rs.getLong("statusId"), null, null)).remarks(rs.getString("remarks"))
                    .resolutionRemarks(rs.getString("resolutionRemarks")).createdDate(rs.getObject("createdDate", LocalDateTime.class))
                    .build();
        }, caseData.getId());

        return KycCaseData.builder().id(caseData.getId()).clientId(caseData.getClientId()).clientName(caseData.getClientName())
                .tier(caseData.getTier()).workflowStatusId(caseData.getWorkflowStatusId())
                .workflowStatusCode(caseData.getWorkflowStatusCode()).workflowStatusValue(caseData.getWorkflowStatusValue())
                .assignedAgentId(caseData.getAssignedAgentId()).assignedAgentName(caseData.getAssignedAgentName())
                .assignedDate(caseData.getAssignedDate()).assignmentStatusId(caseData.getAssignmentStatusId())
                .assignmentStatusCode(caseData.getAssignmentStatusCode()).assignmentStatusValue(caseData.getAssignmentStatusValue())
                .riskLevelId(caseData.getRiskLevelId()).riskLevelCode(caseData.getRiskLevelCode())
                .riskLevelValue(caseData.getRiskLevelValue()).riskScore(caseData.getRiskScore()).riskReason(caseData.getRiskReason())
                .flags(flags).escalationReason(caseData.getEscalationReason()).complianceNotes(caseData.getComplianceNotes())
                .nextReviewDate(caseData.getNextReviewDate()).documents(caseData.getDocuments())
                .requiredDocumentCount(caseData.getRequiredDocumentCount()).submittedDocumentCount(caseData.getSubmittedDocumentCount())
                .completionPercentage(caseData.getCompletionPercentage()).createdDate(caseData.getCreatedDate())
                .createdByUsername(caseData.getCreatedByUsername()).lastModifiedDate(caseData.getLastModifiedDate())
                .lastModifiedByUsername(caseData.getLastModifiedByUsername()).build();
    }

    private static final class KycCaseMapper implements RowMapper<KycCaseData> {

        public String schema() {
            return " c.id AS id, c.client_id AS clientId, cl.display_name AS clientName, "
                    + "COALESCE(c.kyc_tier_id, cl.kyc_tier_id) AS tierId, kt.name AS tierName, "
                    + "c.workflow_status_enum AS workflowStatusId, " + "c.assigned_agent_id AS assignedAgentId, "
                    + "au.username AS assignedAgentName, " + "c.assigned_date AS assignedDate, "
                    + "c.assignment_status_enum AS assignmentStatusId, "
                    + "c.risk_level_enum AS riskLevelId, c.risk_score AS riskScore, c.risk_reason AS riskReason, "
                    + "c.escalation_reason AS escalationReason, c.compliance_notes AS complianceNotes, c.next_review_date AS nextReviewDate, "
                    + "c.created_date AS createdDate, c.lastmodified_date AS lastModifiedDate, "
                    + "c.createdby_id AS createdById, c.lastmodifiedby_id AS lastModifiedById " + "FROM m_kyc_case c "
                    + "JOIN m_client cl ON cl.id = c.client_id "
                    + "LEFT JOIN m_kyc_tier kt ON kt.id = COALESCE(c.kyc_tier_id, cl.kyc_tier_id) "
                    + "LEFT JOIN m_appuser au ON au.id = c.assigned_agent_id ";
        }

        @Override
        public KycCaseData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long clientId = rs.getLong("clientId");
            final String clientName = rs.getString("clientName");

            final Long tierId = JdbcSupport.getLong(rs, "tierId");
            KycTierData tier = null;
            if (tierId != null) {
                final String tierName = rs.getString("tierName");
                tier = KycTierData.builder().id(tierId).name(tierName).build();
            }

            final Integer workflowStatusId = rs.getInt("workflowStatusId");
            final KycCaseWorkflowStatus workflowStatus = KycCaseWorkflowStatus.fromInt(workflowStatusId);

            final Long assignedAgentId = JdbcSupport.getLong(rs, "assignedAgentId");
            final String assignedAgentName = rs.getString("assignedAgentName");
            final java.sql.Timestamp assignedDateTs = rs.getTimestamp("assignedDate");
            final LocalDateTime assignedDate = assignedDateTs != null ? assignedDateTs.toLocalDateTime() : null;
            final Integer assignmentStatusId = rs.getInt("assignmentStatusId");
            final KycAssignmentStatus assignmentStatus = KycAssignmentStatus.fromInt(assignmentStatusId);

            final Integer riskLevelId = rs.getInt("riskLevelId");
            final KycRiskLevel riskLevel = KycRiskLevel.fromInt(riskLevelId);
            final BigDecimal riskScore = rs.getBigDecimal("riskScore");
            final String riskReason = rs.getString("riskReason");

            final String escalationReason = rs.getString("escalationReason");
            final String complianceNotes = rs.getString("complianceNotes");
            final java.time.LocalDate nextReviewDate = rs.getObject("nextReviewDate", java.time.LocalDate.class);

            final LocalDateTime createdDate = rs.getObject("createdDate", LocalDateTime.class);
            final LocalDateTime lastModifiedDate = rs.getObject("lastModifiedDate", LocalDateTime.class);

            return KycCaseData.builder().id(id).clientId(clientId).clientName(clientName).tier(tier).workflowStatusId(workflowStatusId)
                    .workflowStatusCode(workflowStatus.getCode()).workflowStatusValue(workflowStatus.name())
                    .assignedAgentId(assignedAgentId).assignedAgentName(assignedAgentName).assignedDate(assignedDate)
                    .assignmentStatusId(assignmentStatusId).assignmentStatusCode(assignmentStatus.getCode())
                    .assignmentStatusValue(assignmentStatus.name()).riskLevelId(riskLevelId).riskLevelCode(riskLevel.getCode())
                    .riskLevelValue(riskLevel.name()).riskScore(riskScore).riskReason(riskReason).escalationReason(escalationReason)
                    .complianceNotes(complianceNotes).nextReviewDate(nextReviewDate).createdDate(createdDate)
                    .lastModifiedDate(lastModifiedDate).build();
        }
    }

    private static final class KycCaseHistoryMapper implements RowMapper<KycCaseHistoryData> {

        public String schema() {
            return " h.id as id, h.kyc_case_id as kycCaseId, " + " h.from_status_enum as fromStatusId, h.to_status_enum as toStatusId, "
                    + " h.from_assignment_status_enum as fromAssignmentStatusId, h.to_assignment_status_enum as toAssignmentStatusId, "
                    + " h.comments as comments, h.actor_id as actorId, u.username as actorUsername, h.created_date as createdDate "
                    + " from m_kyc_case_history h " + " left join m_appuser u on u.id = h.actor_id ";
        }

        @Override
        public KycCaseHistoryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long kycCaseId = rs.getLong("kycCaseId");

            final Integer fromStatusId = JdbcSupport.getInteger(rs, "fromStatusId");
            final KycCaseWorkflowStatus fromStatus = fromStatusId != null ? KycCaseWorkflowStatus.fromInt(fromStatusId) : null;
            final Integer toStatusId = JdbcSupport.getInteger(rs, "toStatusId");
            final KycCaseWorkflowStatus toStatus = toStatusId != null ? KycCaseWorkflowStatus.fromInt(toStatusId) : null;

            final Integer fromAssignmentStatusId = JdbcSupport.getInteger(rs, "fromAssignmentStatusId");
            final KycAssignmentStatus fromAssignmentStatus = fromAssignmentStatusId != null
                    ? KycAssignmentStatus.fromInt(fromAssignmentStatusId)
                    : null;
            final Integer toAssignmentStatusId = JdbcSupport.getInteger(rs, "toAssignmentStatusId");
            final KycAssignmentStatus toAssignmentStatus = toAssignmentStatusId != null ? KycAssignmentStatus.fromInt(toAssignmentStatusId)
                    : null;

            final String comments = rs.getString("comments");
            final Long actorId = JdbcSupport.getLong(rs, "actorId");
            final String actorUsername = rs.getString("actorUsername");
            final java.sql.Timestamp createdDateTs = rs.getTimestamp("createdDate");
            final LocalDateTime createdDate = createdDateTs != null ? createdDateTs.toLocalDateTime() : null;

            return KycCaseHistoryData.builder().id(id).kycCaseId(kycCaseId).fromStatusId(fromStatusId)
                    .fromStatusCode(fromStatus != null ? fromStatus.getCode() : null)
                    .fromStatusValue(fromStatus != null ? fromStatus.name() : null).toStatusId(toStatusId)
                    .toStatusCode(toStatus != null ? toStatus.getCode() : null).toStatusValue(toStatus != null ? toStatus.name() : null)
                    .fromAssignmentStatusId(fromAssignmentStatusId)
                    .fromAssignmentStatusCode(fromAssignmentStatus != null ? fromAssignmentStatus.getCode() : null)
                    .fromAssignmentStatusValue(fromAssignmentStatus != null ? fromAssignmentStatus.name() : null)
                    .toAssignmentStatusId(toAssignmentStatusId)
                    .toAssignmentStatusCode(toAssignmentStatus != null ? toAssignmentStatus.getCode() : null)
                    .toAssignmentStatusValue(toAssignmentStatus != null ? toAssignmentStatus.name() : null).comments(comments)
                    .actorId(actorId).actorUsername(actorUsername).createdDate(createdDate).build();
        }
    }

    private static final class KycAuditLogMapper implements RowMapper<KycAuditLogData> {

        public String schema() {
            return " a.id as id, a.kyc_case_id as kycCaseId, a.entity_type as entityType, a.entity_id as entityId, "
                    + " a.action as action, a.field_name as fieldName, a.old_value as oldValue, a.new_value as newValue, "
                    + " a.comments as comments, a.actor_id as actorId, u.username as actorUsername, "
                    + " a.ip_address as ipAddress, a.created_date as createdDate " + " from m_kyc_audit_log a "
                    + " left join m_appuser u on u.id = a.actor_id ";
        }

        @Override
        public KycAuditLogData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long kycCaseId = rs.getLong("kycCaseId");
            final String entityType = rs.getString("entityType");
            final Long entityId = JdbcSupport.getLong(rs, "entityId");
            final String action = rs.getString("action");
            final String fieldName = rs.getString("fieldName");
            final String oldValue = rs.getString("oldValue");
            final String newValue = rs.getString("newValue");
            final String comments = rs.getString("comments");
            final Long actorId = JdbcSupport.getLong(rs, "actorId");
            final String actorUsername = rs.getString("actorUsername");
            final String ipAddress = rs.getString("ipAddress");
            final java.sql.Timestamp createdDateTs = rs.getTimestamp("createdDate");
            final LocalDateTime createdDate = createdDateTs != null ? createdDateTs.toLocalDateTime() : null;

            return KycAuditLogData.builder().id(id).kycCaseId(kycCaseId).entityType(entityType).entityId(entityId).action(action)
                    .fieldName(fieldName).oldValue(oldValue).newValue(newValue).comments(comments).actorId(actorId)
                    .actorUsername(actorUsername).ipAddress(ipAddress).createdDate(createdDate).build();
        }
    }
}
