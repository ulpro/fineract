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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.kyc.data.KycDocumentTypeData;
import org.apache.fineract.kyc.domain.KycDocumentTypeRepositoryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KycDocumentTypeReadPlatformServiceImpl implements KycDocumentTypeReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final KycDocumentTypeRepositoryWrapper repositoryWrapper;

    @Override
    public List<KycDocumentTypeData> retrieveAll() {
        final KycDocumentTypeMapper mapper = new KycDocumentTypeMapper();
        return this.jdbcTemplate.query(mapper.schema(), mapper);
    }

    @Override
    public KycDocumentTypeData retrieveOne(final Long id) {
        final KycDocumentTypeMapper mapper = new KycDocumentTypeMapper();
        return this.jdbcTemplate.queryForObject(mapper.schema() + " WHERE dt.id = ?", mapper, id);
    }

    private static final class KycDocumentTypeMapper implements RowMapper<KycDocumentTypeData> {

        public String schema() {
            return "SELECT dt.id, dt.code, dt.name, dt.description, dt.is_active AS active, "
                    + "dt.created_date AS createdDate, dt.lastmodified_date AS lastModifiedDate " + "FROM m_kyc_document_type dt";
        }

        @Override
        public KycDocumentTypeData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String code = rs.getString("code");
            final String name = rs.getString("name");
            final String description = rs.getString("description");
            final boolean active = rs.getBoolean("active");

            final java.sql.Timestamp createdDateTs = rs.getTimestamp("createdDate");
            final LocalDateTime createdDate = createdDateTs != null ? createdDateTs.toLocalDateTime() : null;

            final java.sql.Timestamp lastModifiedDateTs = rs.getTimestamp("lastModifiedDate");
            final LocalDateTime lastModifiedDate = lastModifiedDateTs != null ? lastModifiedDateTs.toLocalDateTime() : null;

            return KycDocumentTypeData.builder().id(id).code(code).name(name).description(description).active(active)
                    .createdDate(createdDate).lastModifiedDate(lastModifiedDate).build();
        }
    }
}
