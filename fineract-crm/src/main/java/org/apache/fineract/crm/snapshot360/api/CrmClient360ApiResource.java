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
package org.apache.fineract.crm.snapshot360.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.snapshot360.data.CrmClient360Data;
import org.apache.fineract.crm.snapshot360.service.CrmClient360ReadPlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/crm/clients/{clientId}/360")
@Component
@Tag(name = "CRM Vue 360 Client", description = "Consolidation et vue 360° du client")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CrmClient360ApiResource {

    private final CrmClient360ReadPlatformService crmClient360ReadPlatformService;

    @GET
    @Operation(summary = "Afficher la Vue 360° d'un client", description = "Consolide les comptes, crédits, réclamations, consentements et score KYC.")
    public Response getClient360View(@PathParam("clientId") final Long clientId) {
        CrmClient360Data data = crmClient360ReadPlatformService.retrieveClient360View(clientId);
        return Response.ok(data).build();
    }
}
