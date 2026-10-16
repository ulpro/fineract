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

import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.kyc.data.KycDashboardStatsData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KycDashboardReadPlatformServiceImpl implements KycDashboardReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public KycDashboardStatsData retrieveStats() {
        final String sql = "SELECT " + "  COUNT(*) as totalCases, "
                + "  SUM(CASE WHEN workflow_status_enum = 100 THEN 1 ELSE 0 END) as initiatedCases, "
                + "  SUM(CASE WHEN workflow_status_enum = 200 THEN 1 ELSE 0 END) as docsPendingCases, "
                + "  SUM(CASE WHEN workflow_status_enum = 300 THEN 1 ELSE 0 END) as docsReceivedCases, "
                + "  SUM(CASE WHEN workflow_status_enum = 400 THEN 1 ELSE 0 END) as verificationInProgressCases, "
                + "  SUM(CASE WHEN workflow_status_enum = 500 THEN 1 ELSE 0 END) as pendingApprovalCases, "
                + "  SUM(CASE WHEN workflow_status_enum = 600 THEN 1 ELSE 0 END) as approvedCases, "
                + "  SUM(CASE WHEN workflow_status_enum = 700 THEN 1 ELSE 0 END) as rejectedCases, "
                + "  SUM(CASE WHEN assignment_status_enum = 100 THEN 1 ELSE 0 END) as unassignedCases, "
                + "  SUM(CASE WHEN assignment_status_enum = 200 THEN 1 ELSE 0 END) as assignedCases, "
                + "  SUM(CASE WHEN assignment_status_enum = 300 THEN 1 ELSE 0 END) as inReviewCases, "
                + "  SUM(CASE WHEN assignment_status_enum = 400 THEN 1 ELSE 0 END) as escalatedCases, "
                + "  SUM(CASE WHEN risk_level_enum = 100 THEN 1 ELSE 0 END) as lowRiskCases, "
                + "  SUM(CASE WHEN risk_level_enum = 200 THEN 1 ELSE 0 END) as mediumRiskCases, "
                + "  SUM(CASE WHEN risk_level_enum = 300 THEN 1 ELSE 0 END) as highRiskCases, "
                + "  SUM(CASE WHEN risk_level_enum = 400 THEN 1 ELSE 0 END) as criticalRiskCases " + "FROM m_kyc_case";

        return this.jdbcTemplate.queryForObject(sql, new KycDashboardStatsMapper());
    }

    private static final class KycDashboardStatsMapper implements RowMapper<KycDashboardStatsData> {

        @Override
        public KycDashboardStatsData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            return KycDashboardStatsData.builder().totalCases(rs.getLong("totalCases")).initiatedCases(rs.getLong("initiatedCases"))
                    .docsPendingCases(rs.getLong("docsPendingCases")).docsReceivedCases(rs.getLong("docsReceivedCases"))
                    .verificationInProgressCases(rs.getLong("verificationInProgressCases"))
                    .pendingApprovalCases(rs.getLong("pendingApprovalCases")).approvedCases(rs.getLong("approvedCases"))
                    .rejectedCases(rs.getLong("rejectedCases")).unassignedCases(rs.getLong("unassignedCases"))
                    .assignedCases(rs.getLong("assignedCases")).inReviewCases(rs.getLong("inReviewCases"))
                    .escalatedCases(rs.getLong("escalatedCases")).lowRiskCases(rs.getLong("lowRiskCases"))
                    .mediumRiskCases(rs.getLong("mediumRiskCases")).highRiskCases(rs.getLong("highRiskCases"))
                    .criticalRiskCases(rs.getLong("criticalRiskCases")).build();
        }
    }
}
