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

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.complaint.data.CrmComplaintData;
import org.apache.fineract.crm.complaint.domain.CrmComplaint;
import org.apache.fineract.crm.complaint.domain.CrmComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CrmComplaintWritePlatformServiceImpl implements CrmComplaintWritePlatformService {

    private final CrmComplaintRepository complaintRepository;

    @Override
    public CrmComplaintData createComplaint(CrmComplaintData dto) {
        String ticketNumber = "TICK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Integer priority = dto.getPriorityEnum() != null ? dto.getPriorityEnum() : 1; // 1 = NORMAL
        Integer status = 0; // 0 = REGISTERED

        // Calcul du délai SLA cible (7.3 du SFT)
        LocalDateTime targetDate;
        if (priority == 3) { // CRITICAL
            targetDate = LocalDateTime.now().plusDays(5);
        } else if (priority == 2) { // HIGH
            targetDate = LocalDateTime.now().plusDays(10);
        } else {
            targetDate = LocalDateTime.now().plusDays(15);
        }

        CrmComplaint complaint = new CrmComplaint(
                ticketNumber,
                dto.getClientId(),
                dto.getAccountId(),
                dto.getTransactionId(),
                dto.getChannelCodeValueId(),
                dto.getCategoryCodeValueId(),
                priority,
                status,
                dto.getAssignedToUserId(),
                dto.getOfficeId(),
                dto.getDescription(),
                dto.getIsFraud(),
                targetDate
        );

        CrmComplaint saved = complaintRepository.save(complaint);
        return mapToData(saved);
    }

    @Override
    public CrmComplaintData resolveComplaint(Long complaintId, String resolutionSummary) {
        CrmComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée id: " + complaintId));

        complaint.setStatusEnum(4); // 4 = RESOLVED
        complaint.setResolutionSummary(resolutionSummary);
        complaint.setResolvedDate(LocalDateTime.now());

        CrmComplaint saved = complaintRepository.save(complaint);
        return mapToData(saved);
    }

    @Override
    public CrmComplaintData closeComplaint(Long complaintId, Integer satisfactionScore) {
        CrmComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée id: " + complaintId));

        complaint.setStatusEnum(5); // 5 = CLOSED
        complaint.setSatisfactionScore(satisfactionScore);
        complaint.setClosedDate(LocalDateTime.now());

        CrmComplaint saved = complaintRepository.save(complaint);
        return mapToData(saved);
    }

    @Override
    public CrmComplaintData escalateComplaint(Long complaintId, String escalationNotes) {
        CrmComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée id: " + complaintId));

        complaint.setStatusEnum(6); // 6 = ESCALATED
        if (complaint.getDescription() != null) {
            complaint.setDescription(complaint.getDescription() + "\n[ESCALADE]: " + escalationNotes);
        } else {
            complaint.setDescription("[ESCALADE]: " + escalationNotes);
        }

        CrmComplaint saved = complaintRepository.save(complaint);
        return mapToData(saved);
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
