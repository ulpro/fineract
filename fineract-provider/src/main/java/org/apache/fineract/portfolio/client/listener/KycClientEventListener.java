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
package org.apache.fineract.portfolio.client.listener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.event.business.BusinessEventListener;
import org.apache.fineract.infrastructure.event.business.domain.client.ClientCreateBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.kyc.service.KycCaseWritePlatformService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycClientEventListener implements BusinessEventListener<ClientCreateBusinessEvent> {

    private final KycCaseWritePlatformService kycCaseWritePlatformService;
    private final BusinessEventNotifierService businessEventNotifierService;

    @PostConstruct
    public void init() {
        this.businessEventNotifierService.addPostBusinessEventListener(ClientCreateBusinessEvent.class, this);
    }

    @Override
    public void onBusinessEvent(ClientCreateBusinessEvent event) {
        Client client = event.get();
        if (client != null) {
            log.info("ClientCreateBusinessEvent received for client ID: {}. Automatically creating KYC Case.", client.getId());
            try {
                kycCaseWritePlatformService.createCaseForClient(client.getId());
                log.info("Successfully created automatic KYC Case for client ID: {}", client.getId());
            } catch (Exception e) {
                log.error("Failed to create automatic KYC Case for client ID: {}", client.getId(), e);
            }
        }
    }
}
