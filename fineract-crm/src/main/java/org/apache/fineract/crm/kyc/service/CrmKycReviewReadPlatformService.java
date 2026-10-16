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
package org.apache.fineract.crm.kyc.service;

import java.util.List;
import org.apache.fineract.crm.kyc.data.CrmKycReviewData;

/**
 * SF-01-06 : Service lecture des revues KYC LBC/FT.
 */
public interface CrmKycReviewReadPlatformService {

    List<CrmKycReviewData> retrieveByProspectId(Long prospectId);

    List<CrmKycReviewData> retrieveByClientId(Long clientId);

    CrmKycReviewData retrieveById(Long reviewId);

    /** SF-01-06 : Revues dont la date de prochaine revue est dépassée. */
    List<CrmKycReviewData> retrieveOverdueReviews();

    /** SF-01-06 : Revues dues dans les N prochains jours. */
    List<CrmKycReviewData> retrieveReviewsDueWithinDays(int days);
}
