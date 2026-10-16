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
package org.apache.fineract.crm.interaction.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.interaction.data.CrmInteractionData;
import org.apache.fineract.crm.interaction.domain.CrmInteraction;
import org.apache.fineract.crm.interaction.domain.CrmInteractionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Path("/v1/crm/interactions")
@Component
@Tag(name = "CRM Interactions", description = "Journalisation des contacts et visites terrain")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CrmInteractionApiResource {

    private final CrmInteractionRepository interactionRepository;

    @GET
    @Operation(summary = "Lister les interactions", description = "Retourne le journal des interactions par client ou pour l'agent.")
    public Response retrieveInteractions(@QueryParam("clientId") final Long clientId, @QueryParam("staffId") final Long staffId) {
        List<CrmInteraction> list;
        if (clientId != null) {
            list = interactionRepository.findByClientId(clientId);
        } else if (staffId != null) {
            list = interactionRepository.findByStaffId(staffId);
        } else {
            list = interactionRepository.findAll();
        }

        List<CrmInteractionData> dataList = list.stream()
                .map(i -> new CrmInteractionData(i.getId(), i.getClientId(), i.getChannelCodeValueId(),
                        i.getStaffId(), i.getSubject(), i.getNotes(), i.getInteractionDate(), i.getLatitude(), i.getLongitude()))
                .collect(Collectors.toList());

        return Response.ok(dataList).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Enregistrer une nouvelle interaction", description = "Trace un contact (appel, visite terrain avec GPS, SMS, RDV).")
    public Response createInteraction(final CrmInteractionData data) {
        CrmInteraction interaction = new CrmInteraction(
                data.getClientId(),
                data.getChannelCodeValueId(),
                data.getStaffId(),
                data.getSubject(),
                data.getNotes(),
                data.getInteractionDate(),
                data.getLatitude(),
                data.getLongitude()
        );

        CrmInteraction saved = interactionRepository.save(interaction);
        CrmInteractionData result = new CrmInteractionData(saved.getId(), saved.getClientId(), saved.getChannelCodeValueId(),
                saved.getStaffId(), saved.getSubject(), saved.getNotes(), saved.getInteractionDate(), saved.getLatitude(), saved.getLongitude());

        return Response.status(Response.Status.CREATED).entity(result).build();
    }
}
