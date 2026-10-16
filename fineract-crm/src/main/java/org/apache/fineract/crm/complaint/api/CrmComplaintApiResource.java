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
package org.apache.fineract.crm.complaint.api;

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
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.complaint.data.CrmComplaintData;
import org.apache.fineract.crm.complaint.service.CrmComplaintReadPlatformService;
import org.apache.fineract.crm.complaint.service.CrmComplaintWritePlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/crm/complaints")
@Component
@Tag(name = "CRM Réclamations", description = "Gestion des tickets de réclamation et SLA")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CrmComplaintApiResource {

    private final CrmComplaintReadPlatformService complaintReadPlatformService;
    private final CrmComplaintWritePlatformService complaintWritePlatformService;

    @GET
    @Operation(summary = "Lister toutes les réclamations", description = "Retourne l'ensemble des tickets de réclamations enregistrés.")
    public Response retrieveAllComplaints(@QueryParam("clientId") final Long clientId) {
        if (clientId != null) {
            List<CrmComplaintData> complaints = complaintReadPlatformService.retrieveComplaintsByClient(clientId);
            return Response.ok(complaints).build();
        }
        List<CrmComplaintData> complaints = complaintReadPlatformService.retrieveAllComplaints();
        return Response.ok(complaints).build();
    }

    @GET
    @Path("/{complaintId}")
    @Operation(summary = "Obtenir une réclamation par son ID", description = "Retourne le détail d'une réclamation spécifiée par son ID.")
    public Response retrieveComplaintById(@PathParam("complaintId") final Long complaintId) {
        CrmComplaintData complaint = complaintReadPlatformService.retrieveComplaintById(complaintId);
        return Response.ok(complaint).build();
    }

    @GET
    @Path("/ticket/{ticketNumber}")
    @Operation(summary = "Obtenir une réclamation par numéro de ticket", description = "Retourne le détail d'une réclamation par son numéro de ticket unique.")
    public Response retrieveComplaintByTicketNumber(@PathParam("ticketNumber") final String ticketNumber) {
        CrmComplaintData complaint = complaintReadPlatformService.retrieveComplaintByTicketNumber(ticketNumber);
        return Response.ok(complaint).build();
    }

    @GET
    @Path("/fraud")
    @Operation(summary = "Lister les réclamations de catégorie Fraude", description = "Zone cloisonnée réservée au rôle Conformité.")
    public Response retrieveFraudComplaints() {
        List<CrmComplaintData> frauds = complaintReadPlatformService.retrieveFraudComplaints();
        return Response.ok(frauds).build();
    }

    @POST
    @Operation(summary = "Enregistrer une nouvelle réclamation", description = "Crée un ticket avec attribution automatique de numéro unique et calcul SLA.")
    public Response createComplaint(final CrmComplaintData complaintData) {
        CrmComplaintData created = complaintWritePlatformService.createComplaint(complaintData);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("/{complaintId}/resolve")
    @Operation(summary = "Résoudre une réclamation", description = "Enregistre la solution proposée pour la réclamation.")
    public Response resolveComplaint(@PathParam("complaintId") final Long complaintId, final CrmComplaintData data) {
        CrmComplaintData resolved = complaintWritePlatformService.resolveComplaint(complaintId, data.getResolutionSummary());
        return Response.ok(resolved).build();
    }

    @POST
    @Path("/{complaintId}/close")
    @Operation(summary = "Clôturer une réclamation", description = "Clôture la réclamation avec note de satisfaction client (1 à 5).")
    public Response closeComplaint(@PathParam("complaintId") final Long complaintId, final CrmComplaintData data) {
        CrmComplaintData closed = complaintWritePlatformService.closeComplaint(complaintId, data.getSatisfactionScore());
        return Response.ok(closed).build();
    }

    @POST
    @Path("/{complaintId}/escalate")
    @Operation(summary = "Escalader une réclamation", description = "Escalade automatique ou manuelle de la réclamation vers le niveau supérieur.")
    public Response escalateComplaint(@PathParam("complaintId") final Long complaintId, final CrmComplaintData data) {
        CrmComplaintData escalated = complaintWritePlatformService.escalateComplaint(complaintId, data.getDescription());
        return Response.ok(escalated).build();
    }
}
