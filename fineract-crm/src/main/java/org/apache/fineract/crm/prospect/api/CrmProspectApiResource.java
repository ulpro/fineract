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
package org.apache.fineract.crm.prospect.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.prospect.data.CrmDuplicateMatchData;
import org.apache.fineract.crm.prospect.data.CrmProspectData;
import org.apache.fineract.crm.prospect.service.CrmProspectReadPlatformService;
import org.apache.fineract.crm.prospect.service.CrmProspectWritePlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/crm/prospects")
@Component
@Tag(name = "CRM Prospects", description = "Gestion des prospects et dédoublonnage CRM")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CrmProspectApiResource {

    private final CrmProspectReadPlatformService prospectReadPlatformService;
    private final CrmProspectWritePlatformService prospectWritePlatformService;

    @GET
    @Operation(summary = "Lister les prospects CRM", description = "Retourne la liste de tous les prospects enregistrés dans le CRM.")
    public Response retrieveAllProspects() {
        List<CrmProspectData> prospects = prospectReadPlatformService.retrieveAllProspects();
        return Response.ok(prospects).build();
    }

    @GET
    @Path("/{prospectId}")
    @Operation(summary = "Obtenir un prospect par son ID", description = "Retourne la fiche prospect correspondant à l'ID spécifié.")
    public Response retrieveProspectById(@PathParam("prospectId") final Long prospectId) {
        CrmProspectData prospect = prospectReadPlatformService.retrieveProspectById(prospectId);
        return Response.ok(prospect).build();
    }

    @POST
    @Operation(summary = "Créer un nouveau prospect", description = "Création d'un prospect avec vérification automatique des doublons.")
    public Response createProspect(final CrmProspectData prospectData) {
        CrmProspectData created = prospectWritePlatformService.createProspect(prospectData);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{prospectId}")
    @Operation(summary = "Mettre à jour un prospect", description = "Met à jour les informations d'un prospect existant.")
    public Response updateProspect(@PathParam("prospectId") final Long prospectId, final CrmProspectData prospectData) {
        CrmProspectData updated = prospectWritePlatformService.updateProspect(prospectId, prospectData);
        return Response.ok(updated).build();
    }

    @POST
    @Path("/{prospectId}/convert")
    @Operation(summary = "Convertir un prospect en client", description = "Marque le prospect comme converti et le prépare à l'activation Fineract.")
    public Response convertProspectToClient(@PathParam("prospectId") final Long prospectId) {
        CrmProspectData converted = prospectWritePlatformService.convertProspectToClient(prospectId);
        return Response.ok(converted).build();
    }

    @GET
    @Path("/check-duplicates")
    @Operation(summary = "Vérifier la présence de doublons",
               description = "SF-01-03 : Vérification en temps réel dans crm_prospect ET m_client (Fineract). Critères : nationalId, mobile, email, nom+prénom.")
    public Response checkForDuplicates(
            @QueryParam("firstname") final String firstname,
            @QueryParam("lastname") final String lastname,
            @QueryParam("mobileNo") final String mobileNo,
            @QueryParam("email") final String email,
            @QueryParam("nationalId") final String nationalId) {
        CrmDuplicateMatchData match = prospectReadPlatformService.checkForDuplicates(firstname, lastname, mobileNo, email, nationalId);
        return Response.ok(match).build();
    }
}
