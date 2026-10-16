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
package org.apache.fineract.crm.complaint.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.complaint.data.CrmComplaintData;
import org.apache.fineract.crm.complaint.domain.CrmComplaint;
import org.apache.fineract.crm.complaint.domain.CrmComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmComplaintReadPlatformServiceImpl implements CrmComplaintReadPlatformService {

    private final CrmComplaintRepository complaintRepository;

    @Override
    public List<CrmComplaintData> retrieveAllComplaints() {
        return complaintRepository.findAll().stream()
                .map(this::mapToData)
                .collect(Collectors.toList());
    }

    @Override
    public List<CrmComplaintData> retrieveComplaintsByClient(Long clientId) {
        return complaintRepository.findByClientId(clientId).stream()
                .map(this::mapToData)
                .collect(Collectors.toList());
    }

    @Override
    public CrmComplaintData retrieveComplaintByTicketNumber(String ticketNumber) {
        CrmComplaint c = complaintRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée ticket: " + ticketNumber));
        return mapToData(c);
    }

    @Override
    public CrmComplaintData retrieveComplaintById(Long complaintId) {
        CrmComplaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée id: " + complaintId));
        return mapToData(c);
    }

    @Override
    public List<CrmComplaintData> retrieveFraudComplaints() {
        return complaintRepository.findByIsFraudTrue().stream()
                .map(this::mapToData)
                .collect(Collectors.toList());
    }

    private CrmComplaintData mapToData(CrmComplaint c) {
        return new CrmComplaintData(
                c.getId(),
                c.getTicketNumber(),
                c.getClientId(),
                c.getAccountId(),
                c.getTransactionId(),
                c.getChannelCodeValueId(),
                c.getCategoryCodeValueId(),
                c.getPriorityEnum(),
                c.getStatusEnum(),
                c.getAssignedToUserId(),
                c.getOfficeId(),
                c.getDescription(),
                c.getResolutionSummary(),
                c.getSatisfactionScore(),
                c.getIsFraud(),
                c.getCreatedDate(),
                c.getTargetResolutionDate(),
                c.getResolvedDate(),
                c.getClosedDate()
        );
    }
}
