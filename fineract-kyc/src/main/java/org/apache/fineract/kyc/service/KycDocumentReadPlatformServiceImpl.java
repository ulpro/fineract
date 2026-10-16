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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.contentstore.service.ContentStoreService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.documentmanagement.data.DocumentContent;
import org.apache.fineract.kyc.data.KycDocumentData;
import org.apache.fineract.kyc.domain.KycDocumentRepositoryWrapper;
import org.apache.fineract.kyc.domain.KycDocumentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KycDocumentReadPlatformServiceImpl implements KycDocumentReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final KycDocumentRepositoryWrapper repositoryWrapper;
    private final ContentStoreService contentStoreService;

    @Override
    public List<KycDocumentData> retrieveAllByClientId(final Long clientId) {
        final KycDocumentMapper mapper = new KycDocumentMapper();
        return this.jdbcTemplate.query(mapper.schema() + " WHERE d.client_id = ?", mapper, clientId);
    }

    @Override
    public KycDocumentData retrieveOne(final Long documentId) {
        final KycDocumentMapper mapper = new KycDocumentMapper();
        final List<KycDocumentData> results = this.jdbcTemplate.query(mapper.schema() + " WHERE d.id = ?", mapper, documentId);
        if (results.isEmpty()) {
            this.repositoryWrapper.findOneWithNotFoundDetection(documentId);
        }
        return results.get(0);
    }

    @Override
    public DocumentContent retrieveDocumentContent(final Long documentId) {
        final KycDocumentData document = retrieveOne(documentId);

        final String fileLocation = document.getFileLocation();

        // Ensure the fileLocation is valid
        if (fileLocation == null || fileLocation.isEmpty()) {
            throw new org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException(
                    "error.msg.document.fileLocation.empty", "Document file location is missing.");
        }

        final java.io.InputStream inputStream = this.contentStoreService.download(fileLocation);

        // Extract fileName from the fileLocation (assumes standard Fineract format)
        String fileName = "document";
        if (fileLocation.contains("/")) {
            fileName = fileLocation.substring(fileLocation.lastIndexOf('/') + 1);
        } else if (fileLocation.contains("\\")) {
            fileName = fileLocation.substring(fileLocation.lastIndexOf('\\') + 1);
        }

        return DocumentContent.builder().fileName(fileName).contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .stream(inputStream).build();
    }

    private static final class KycDocumentMapper implements RowMapper<KycDocumentData> {

        public String schema() {
            return "SELECT d.id, d.client_id AS clientId, d.kyc_document_type_id AS documentTypeId, "
                    + "dt.code AS documentTypeCode, dt.name AS documentTypeName, d.document_number AS documentNumber, "
                    + "d.issue_date AS issueDate, d.expiry_date AS expiryDate, d.file_location AS fileLocation, "
                    + "d.verification_status_enum AS verificationStatusId, d.remarks, d.created_date AS createdDate, "
                    + "d.lastmodified_date AS lastModifiedDate " + "FROM m_kyc_document d "
                    + "JOIN m_kyc_document_type dt ON dt.id = d.kyc_document_type_id";
        }

        @Override
        public KycDocumentData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long clientId = rs.getLong("clientId");
            final Long documentTypeId = rs.getLong("documentTypeId");
            final String documentTypeCode = rs.getString("documentTypeCode");
            final String documentTypeName = rs.getString("documentTypeName");
            final String documentNumber = rs.getString("documentNumber");
            final LocalDate issueDate = rs.getObject("issueDate", LocalDate.class);
            final LocalDate expiryDate = rs.getObject("expiryDate", LocalDate.class);
            final String fileLocation = rs.getString("fileLocation");
            final Integer verificationStatusId = rs.getInt("verificationStatusId");
            final String remarks = rs.getString("remarks");
            final LocalDateTime createdDate = rs.getObject("createdDate", LocalDateTime.class);
            final LocalDateTime lastModifiedDate = rs.getObject("lastModifiedDate", LocalDateTime.class);

            final KycDocumentStatus status = KycDocumentStatus.fromInt(verificationStatusId);
            final EnumOptionData statusData = new EnumOptionData(status.getValue().longValue(), status.getCode(), status.name());

            return KycDocumentData.builder().id(id).clientId(clientId).documentTypeId(documentTypeId).documentTypeCode(documentTypeCode)
                    .documentTypeName(documentTypeName).documentNumber(documentNumber).issueDate(issueDate).expiryDate(expiryDate)
                    .fileLocation(fileLocation).verificationStatus(statusData).remarks(remarks).createdDate(createdDate)
                    .lastModifiedDate(lastModifiedDate).build();
        }
    }
}
