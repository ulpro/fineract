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
package org.apache.fineract.crm.kyc.api;

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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.kyc.data.CrmKycReviewData;
import org.apache.fineract.crm.kyc.service.CrmKycReviewReadPlatformService;
import org.apache.fineract.crm.kyc.service.CrmKycReviewWritePlatformService;
import org.springframework.stereotype.Component;

/**
 * SF-01-06 : API REST pour les revues KYC LBC/FT.
 *
 * <pre>
 * GET  /crm/kyc/prospects/{prospectId}/reviews   — revues d'un prospect
 * GET  /crm/kyc/clients/{clientId}/reviews        — revues d'un client
 * GET  /crm/kyc/reviews/{reviewId}                — une revue par ID
 * POST /crm/kyc/reviews                           — créer une revue
 * POST /crm/kyc/reviews/{reviewId}/validate       — valider (statut → VALIDEE)
 * POST /crm/kyc/reviews/{reviewId}/reject         — rejeter (statut → REJETEE)
 * GET  /crm/kyc/reviews/overdue                   — revues en retard
 * GET  /crm/kyc/reviews/due?days=30               — revues dues dans N jours
 * </pre>
 */
@Path("/crm/kyc")
@Component
@RequiredArgsConstructor
@Tag(name = "CRM KYC Reviews", description = "SF-01-06 : Gestion des revues KYC périodiques LBC/FT")
public class CrmKycReviewApiResource {

    private final CrmKycReviewReadPlatformService readService;
    private final CrmKycReviewWritePlatformService writeService;

    @GET
    @Path("/prospects/{prospectId}/reviews")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-06 : Lister les revues KYC d'un prospect")
    public List<CrmKycReviewData> getByProspect(@PathParam("prospectId") Long prospectId) {
        return readService.retrieveByProspectId(prospectId);
    }

    @GET
    @Path("/clients/{clientId}/reviews")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-06 : Lister les revues KYC d'un client")
    public List<CrmKycReviewData> getByClient(@PathParam("clientId") Long clientId) {
        return readService.retrieveByClientId(clientId);
    }

    @GET
    @Path("/reviews/{reviewId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-06 : Récupérer une revue KYC par son ID")
    public CrmKycReviewData getById(@PathParam("reviewId") Long reviewId) {
        return readService.retrieveById(reviewId);
    }

    @GET
    @Path("/reviews/overdue")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-06 : Revues KYC en retard (prochaine date dépassée)")
    public List<CrmKycReviewData> getOverdue() {
        return readService.retrieveOverdueReviews();
    }

    @GET
    @Path("/reviews/due")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-06 : Revues KYC dues dans les N prochains jours (défaut 30)")
    public List<CrmKycReviewData> getDue(@QueryParam("days") Integer days) {
        return readService.retrieveReviewsDueWithinDays(days != null ? days : 30);
    }

    @POST
    @Path("/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-06 : Créer une revue KYC LBC/FT")
    public CrmKycReviewData create(CrmKycReviewData dto) {
        return writeService.createReview(dto);
    }

    @POST
    @Path("/reviews/{reviewId}/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-06 : Valider une revue KYC (statut → VALIDEE)")
    public CrmKycReviewData validate(@PathParam("reviewId") Long reviewId, CrmKycReviewData dto) {
        return writeService.validateReview(reviewId, dto.getRiskLevelEnum(),
                dto.getReviewNotes(), dto.getReviewedByUserId());
    }

    @POST
    @Path("/reviews/{reviewId}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-06 : Rejeter une revue KYC (statut → REJETEE)")
    public CrmKycReviewData reject(@PathParam("reviewId") Long reviewId, CrmKycReviewData dto) {
        return writeService.rejectReview(reviewId, dto.getReviewNotes(), dto.getReviewedByUserId());
    }
}
