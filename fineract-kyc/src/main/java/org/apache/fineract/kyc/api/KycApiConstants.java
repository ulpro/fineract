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
package org.apache.fineract.kyc.api;

public interface KycApiConstants {

    String codeParamName = "code";
    String nameParamName = "name";
    String descriptionParamName = "description";
    String activeParamName = "active";
    String escalationReasonParamName = "escalationReason";
    String complianceNotesParamName = "complianceNotes";

    // Common params
    String localeParamName = "locale";
    String dateFormatParamName = "dateFormat";

    // KycTier params
    String levelParamName = "level";
    String maxBalanceParamName = "maxBalance";
    String maxTransactionAmountParamName = "maxTransactionAmount";
    String validityDaysParamName = "validityDays";
    String clientTypeCvIdParamName = "clientTypeCvId";
    String documentRequirementsParamName = "documentRequirements";
    String requiredFieldsParamName = "requiredFields";
    String documentTypeIdParamName = "documentTypeId";
    String mandatoryParamName = "mandatory";

    // KycDocument params
    String documentNumberParamName = "documentNumber";
    String issueDateParamName = "issueDate";
    String expiryDateParamName = "expiryDate";
    String fileLocationParamName = "fileLocation";
    String verificationStatusParamName = "verificationStatus";
    String remarksParamName = "remarks";

    String KYC_DOCUMENT_TYPE_RESOURCE_NAME = "KYCDOCUMENTTYPE";
    String KYC_TIER_RESOURCE_NAME = "KYCTIER";
    String KYC_DOCUMENT_RESOURCE_NAME = "KYCDOCUMENT";

    // KycCase params
    String clientIdParamName = "clientId";
    String tierIdParamName = "tierId";
    String workflowStatusParamName = "workflowStatus";
    String commentsParamName = "comments";
    String riskLevelParamName = "riskLevel";
    String riskScoreParamName = "riskScore";
    String riskReasonParamName = "riskReason";
    String riskFlagsParamName = "riskFlags";
    String assignedAgentIdParamName = "assignedAgentId";
    String assignmentStatusParamName = "assignmentStatus";

    String KYC_CASE_RESOURCE_NAME = "KYCCASE";
}
