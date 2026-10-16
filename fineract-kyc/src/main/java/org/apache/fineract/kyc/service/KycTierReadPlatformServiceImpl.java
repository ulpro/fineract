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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.kyc.data.KycTierData;
import org.apache.fineract.kyc.data.KycTierDocumentRequirementData;
import org.apache.fineract.kyc.domain.KycTierRepositoryWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KycTierReadPlatformServiceImpl implements KycTierReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final KycTierRepositoryWrapper repositoryWrapper;

    private static final String BASE_SQL = "SELECT t.id, t.code, t.name, t.description, t.level, t.max_balance AS maxBalance, "
            + "t.max_transaction_amount AS maxTransactionAmount, t.is_active AS active, t.validity_days AS validityDays, t.client_type_cv_id AS clientTypeCvId, "
            + "t.created_date AS createdDate, t.lastmodified_date AS lastModifiedDate, " + "r.id AS reqId, r.is_mandatory AS reqMandatory, "
            + "dt.id AS docTypeId, dt.code AS docTypeCode, dt.name AS docTypeName, " + "rf.id AS reqFieldId, rf.field_name AS reqFieldName "
            + "FROM m_kyc_tier t " + "LEFT JOIN m_kyc_tier_document_requirement r ON r.kyc_tier_id = t.id "
            + "LEFT JOIN m_kyc_document_type dt ON dt.id = r.kyc_document_type_id "
            + "LEFT JOIN m_kyc_tier_required_field rf ON rf.kyc_tier_id = t.id ";

    @Override
    public List<KycTierData> retrieveAll() {
        return this.jdbcTemplate.query(BASE_SQL + "ORDER BY t.level ASC", new KycTierExtractor());
    }

    @Override
    public List<KycTierData> retrieveAllActive() {
        return this.jdbcTemplate.query(BASE_SQL + "WHERE t.is_active = true ORDER BY t.level ASC", new KycTierExtractor());
    }

    @Override
    public KycTierData retrieveOne(final Long id) {
        final List<KycTierData> results = this.jdbcTemplate.query(BASE_SQL + "WHERE t.id = ? ORDER BY t.level ASC", new KycTierExtractor(),
                id);
        if (results.isEmpty()) {
            repositoryWrapper.findOneWithNotFoundDetection(id); // triggers 404
        }
        return results.get(0);
    }

    private static final class KycTierExtractor implements ResultSetExtractor<List<KycTierData>> {

        @Override
        public List<KycTierData> extractData(final ResultSet rs) throws SQLException, DataAccessException {
            final Map<Long, KycTierData> tierMap = new LinkedHashMap<>();
            while (rs.next()) {
                final Long tierId = rs.getLong("id");
                KycTierData tier = tierMap.get(tierId);
                if (tier == null) {
                    tier = KycTierData.builder().id(tierId).code(rs.getString("code")).name(rs.getString("name"))
                            .description(rs.getString("description")).level(rs.getInt("level")).maxBalance(rs.getBigDecimal("maxBalance"))
                            .maxTransactionAmount(rs.getBigDecimal("maxTransactionAmount")).active(rs.getBoolean("active"))
                            .validityDays(rs.getObject("validityDays", Integer.class))
                            .clientTypeCvId(rs.getObject("clientTypeCvId", Long.class))
                            .createdDate(rs.getObject("createdDate", LocalDateTime.class))
                            .lastModifiedDate(rs.getObject("lastModifiedDate", LocalDateTime.class)).documentRequirements(new ArrayList<>())
                            .requiredFields(new ArrayList<>()).build();
                    tierMap.put(tierId, tier);
                }
                final long reqId = rs.getLong("reqId");
                if (reqId > 0) {
                    boolean exists = tier.getDocumentRequirements().stream().anyMatch(r -> r.getId() == reqId);
                    if (!exists) {
                        final KycTierDocumentRequirementData req = KycTierDocumentRequirementData.builder().id(reqId)
                                .documentTypeId(rs.getLong("docTypeId")).documentTypeCode(rs.getString("docTypeCode"))
                                .documentTypeName(rs.getString("docTypeName")).mandatory(rs.getBoolean("reqMandatory")).build();
                        tier.getDocumentRequirements().add(req);
                    }
                }
                final long reqFieldId = rs.getLong("reqFieldId");
                if (reqFieldId > 0) {
                    final String fieldName = rs.getString("reqFieldName");
                    if (!tier.getRequiredFields().contains(fieldName)) {
                        tier.getRequiredFields().add(fieldName);
                    }
                }
            }
            return new ArrayList<>(tierMap.values());
        }
    }
}
