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
package org.apache.fineract.crm.complaint.data;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrmComplaintData implements Serializable {

    private Long id;
    private String ticketNumber;
    private Long clientId;
    private Long accountId;
    private Long transactionId;
    private Long channelCodeValueId;
    private Long categoryCodeValueId;
    private Integer priorityEnum;
    private Integer statusEnum;
    private Long assignedToUserId;
    private Long officeId;
    private String description;
    private String resolutionSummary;
    private Integer satisfactionScore;
    private Boolean isFraud;
    private LocalDateTime createdDate;
    private LocalDateTime targetResolutionDate;
    private LocalDateTime resolvedDate;
    private LocalDateTime closedDate;
}
