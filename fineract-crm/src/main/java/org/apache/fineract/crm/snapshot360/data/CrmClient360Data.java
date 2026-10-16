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
package org.apache.fineract.crm.snapshot360/data;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fineract.crm.consent.domain.CrmConsent;
import org.apache.fineract.crm.prospect.data.CrmProspectData;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrmClient360Data implements Serializable {

    private Long clientId;
    private String clientAccountNo;
    private String firstname;
    private String lastname;
    private String status;
    private Integer kycCompletenessScore; // 0 - 100%
    private String riskLevel; // FAIBLE, MOYEN, ELEVE
    private Integer loyaltyPoints;
    private String loyaltyTier;
    private Integer activeLoansCount;
    private Integer activeSavingsCount;
    private Integer openComplaintsCount;
    private List<CrmConsent> consents;
    private List<CrmProspectData> prospectHistory;
}
