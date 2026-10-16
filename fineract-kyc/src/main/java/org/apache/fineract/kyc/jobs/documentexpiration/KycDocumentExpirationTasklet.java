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
package org.apache.fineract.kyc.jobs.documentexpiration;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.database.DatabaseTypeResolver;
import org.apache.fineract.infrastructure.core.service.database.RoutingDataSourceServiceFactory;
import org.apache.fineract.kyc.domain.KycCaseWorkflowStatus;
import org.apache.fineract.kyc.domain.KycDocumentStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@RequiredArgsConstructor
public class KycDocumentExpirationTasklet implements Tasklet {

    private final RoutingDataSourceServiceFactory dataSourceServiceFactory;
    private final DatabaseTypeResolver databaseTypeResolver;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());
        LocalDate businessDate = DateUtils.getBusinessLocalDate();

        log.info("Running KycDocumentExpirationTasklet for date: {}", businessDate);

        // 1. Mark documents as expired
        String updateDocsSql = "UPDATE m_kyc_document SET status_enum = ?, last_modified_on_utc = ? "
                + "WHERE status_enum = ? AND expiry_date < ?";

        int docsUpdated = jdbcTemplate.update(updateDocsSql, KycDocumentStatus.EXPIRED.getValue(), DateUtils.getAuditOffsetDateTime(),
                KycDocumentStatus.VERIFIED.getValue(), businessDate);

        log.info("Documents marked as expired: {}", docsUpdated);

        if (docsUpdated > 0) {
            // Insert DOCUMENT_EXPIRED flag for cases that don't have it active
            String insertDocExpiredFlagSql = "INSERT INTO m_kyc_case_flag (kyc_case_id, kyc_flag_type_id, status_enum, created_date) "
                    + "SELECT kc.id, ft.id, 100, CURRENT_TIMESTAMP " + "FROM m_kyc_case kc "
                    + "INNER JOIN (SELECT DISTINCT client_id FROM m_kyc_document WHERE status_enum = ?) expired_docs ON kc.client_id = expired_docs.client_id "
                    + "CROSS JOIN m_kyc_flag_type ft " + "WHERE ft.code = 'DOCUMENT_EXPIRED' AND kc.workflow_status_enum = ? "
                    + "AND NOT EXISTS (SELECT 1 FROM m_kyc_case_flag f WHERE f.kyc_case_id = kc.id AND f.kyc_flag_type_id = ft.id AND f.status_enum = 100)";

            int flagDocExpiredInserted = jdbcTemplate.update(insertDocExpiredFlagSql, KycDocumentStatus.EXPIRED.getValue(),
                    KycCaseWorkflowStatus.CONFORME.getValue());
            log.info("DOCUMENT_EXPIRED flags inserted: {}", flagDocExpiredInserted);

            // Transition KYC cases to EN_REVISION if they are CONFORME and have EXPIRED documents
            String updateCasesSql;
            if (databaseTypeResolver.isPostgreSQL()) {
                updateCasesSql = "UPDATE m_kyc_case kc SET workflow_status_enum = ?, last_modified_on_utc = ? "
                        + "FROM (SELECT DISTINCT client_id FROM m_kyc_document WHERE status_enum = ?) expired_docs "
                        + "WHERE kc.client_id = expired_docs.client_id AND kc.workflow_status_enum = ?";

                int casesUpdated = jdbcTemplate.update(updateCasesSql, KycCaseWorkflowStatus.EN_REVISION.getValue(),
                        DateUtils.getAuditOffsetDateTime(), KycDocumentStatus.EXPIRED.getValue(),
                        KycCaseWorkflowStatus.CONFORME.getValue());
                log.info("KYC Cases transitioned to EN_REVISION: {}", casesUpdated);
            } else {
                updateCasesSql = "UPDATE m_kyc_case kc "
                        + "INNER JOIN (SELECT DISTINCT client_id FROM m_kyc_document WHERE status_enum = ?) expired_docs "
                        + "ON kc.client_id = expired_docs.client_id SET kc.workflow_status_enum = ?, kc.last_modified_on_utc = ? "
                        + "WHERE kc.workflow_status_enum = ?";

                int casesUpdated = jdbcTemplate.update(updateCasesSql, KycDocumentStatus.EXPIRED.getValue(),
                        KycCaseWorkflowStatus.EN_REVISION.getValue(), DateUtils.getAuditOffsetDateTime(),
                        KycCaseWorkflowStatus.CONFORME.getValue());
                log.info("KYC Cases transitioned to EN_REVISION: {}", casesUpdated);
            }
        }

        // 3. Handle documents expiring soon (<= 30 days) -> add EXPIRING_SOON flag
        LocalDate warningDate = businessDate.plusDays(30);
        String insertExpiringSoonFlagSql = "INSERT INTO m_kyc_case_flag (kyc_case_id, kyc_flag_type_id, status_enum, created_date) "
                + "SELECT kc.id, ft.id, 100, CURRENT_TIMESTAMP " + "FROM m_kyc_case kc "
                + "INNER JOIN (SELECT DISTINCT client_id FROM m_kyc_document WHERE status_enum = ? AND expiry_date <= ?) soon_docs ON kc.client_id = soon_docs.client_id "
                + "CROSS JOIN m_kyc_flag_type ft " + "WHERE ft.code = 'EXPIRING_SOON' AND kc.workflow_status_enum = ? "
                + "AND NOT EXISTS (SELECT 1 FROM m_kyc_case_flag f WHERE f.kyc_case_id = kc.id AND f.kyc_flag_type_id = ft.id AND f.status_enum = 100)";

        int flagExpiringSoonInserted = jdbcTemplate.update(insertExpiringSoonFlagSql, KycDocumentStatus.VERIFIED.getValue(), warningDate,
                KycCaseWorkflowStatus.CONFORME.getValue());
        log.info("EXPIRING_SOON flags inserted: {}", flagExpiringSoonInserted);

        // 4. Handle Periodic Reviews (next_review_date <= today)
        String insertReviewDueFlagSql = "INSERT INTO m_kyc_case_flag (kyc_case_id, kyc_flag_type_id, status_enum, created_date) "
                + "SELECT kc.id, ft.id, 100, CURRENT_TIMESTAMP " + "FROM m_kyc_case kc " + "CROSS JOIN m_kyc_flag_type ft "
                + "WHERE ft.code = 'PERIODIC_REVIEW_DUE' AND kc.next_review_date <= ? AND kc.workflow_status_enum = ? "
                + "AND NOT EXISTS (SELECT 1 FROM m_kyc_case_flag f WHERE f.kyc_case_id = kc.id AND f.kyc_flag_type_id = ft.id AND f.status_enum = 100)";

        int flagReviewDueInserted = jdbcTemplate.update(insertReviewDueFlagSql, businessDate, KycCaseWorkflowStatus.CONFORME.getValue());
        log.info("PERIODIC_REVIEW_DUE flags inserted: {}", flagReviewDueInserted);

        String updatePeriodicReviewSql = "UPDATE m_kyc_case SET workflow_status_enum = ?, last_modified_on_utc = ? "
                + "WHERE next_review_date <= ? AND workflow_status_enum = ?";

        int reviewCasesUpdated = jdbcTemplate.update(updatePeriodicReviewSql, KycCaseWorkflowStatus.EN_REVISION.getValue(),
                DateUtils.getAuditOffsetDateTime(), businessDate, KycCaseWorkflowStatus.CONFORME.getValue());
        log.info("KYC Cases transitioned to EN_REVISION due to Periodic Review: {}", reviewCasesUpdated);

        return RepeatStatus.FINISHED;
    }
}
