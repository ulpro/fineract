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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.kyc.data.CrmKycDocumentData;
import org.apache.fineract.crm.kyc.service.CrmKycDocumentReadPlatformService;
import org.apache.fineract.crm.kyc.service.CrmKycDocumentWritePlatformService;
import org.springframework.stereotype.Component;

/**
 * SF-01-04 : API REST pour la gestion des pièces KYC.
 * SF-01-05 : Inclut les endpoints d'alerte expiration.
 *
 * <pre>
 * GET    /crm/kyc/prospects/{prospectId}/documents         — pièces d'un prospect
 * GET    /crm/kyc/clients/{clientId}/documents             — pièces d'un client
 * GET    /crm/kyc/documents/{documentId}                   — une pièce par ID
 * POST   /crm/kyc/documents                                — créer une pièce
 * PUT    /crm/kyc/documents/{documentId}                   — mettre à jour
 * DELETE /crm/kyc/documents/{documentId}                   — supprimer
 * POST   /crm/kyc/documents/{documentId}/verify            — vérifier (SF-01-06)
 * GET    /crm/kyc/documents/expiring?days=30               — alertes expiration (SF-01-05)
 * GET    /crm/kyc/documents/expired                        — pièces expirées
 * POST   /crm/kyc/documents/mark-expired                   — job: marquer les expirées
 * </pre>
 */
@Path("/crm/kyc")
@Component
@RequiredArgsConstructor
@Tag(name = "CRM KYC Documents", description = "SF-01-04/05/06 : Gestion des pièces justificatives KYC")
public class CrmKycDocumentApiResource {

    private final CrmKycDocumentReadPlatformService readService;
    private final CrmKycDocumentWritePlatformService writeService;

    // ─── Lecture par prospect ────────────────────────────────────────────────

    @GET
    @Path("/prospects/{prospectId}/documents")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-04 : Lister les pièces KYC d'un prospect")
    public List<CrmKycDocumentData> getByProspect(
            @Parameter(description = "ID du prospect") @PathParam("prospectId") Long prospectId) {
        return readService.retrieveByProspectId(prospectId);
    }

    // ─── Lecture par client Fineract ─────────────────────────────────────────

    @GET
    @Path("/clients/{clientId}/documents")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-04 : Lister les pièces KYC d'un client Fineract")
    public List<CrmKycDocumentData> getByClient(
            @Parameter(description = "ID du client Fineract") @PathParam("clientId") Long clientId) {
        return readService.retrieveByClientId(clientId);
    }

    // ─── Lecture par ID ──────────────────────────────────────────────────────

    @GET
    @Path("/documents/{documentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-04 : Récupérer une pièce KYC par son ID")
    public CrmKycDocumentData getById(
            @PathParam("documentId") Long documentId) {
        return readService.retrieveById(documentId);
    }

    // ─── Alertes expiration (SF-01-05) ───────────────────────────────────────

    @GET
    @Path("/documents/expiring")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-05 : Pièces expirant dans les N prochains jours (défaut 30)")
    public List<CrmKycDocumentData> getExpiring(
            @QueryParam("days") Integer days) {
        return readService.retrieveExpiringWithinDays(days != null ? days : 30);
    }

    @GET
    @Path("/documents/expired")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-05 : Pièces déjà expirées (date dépassée, statut encore ACTIF)")
    public List<CrmKycDocumentData> getExpired() {
        return readService.retrieveExpiredDocuments();
    }

    // ─── Création ────────────────────────────────────────────────────────────

    @POST
    @Path("/documents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-04 : Enregistrer une nouvelle pièce KYC")
    public CrmKycDocumentData create(CrmKycDocumentData dto) {
        return writeService.createDocument(dto);
    }

    // ─── Mise à jour ─────────────────────────────────────────────────────────

    @PUT
    @Path("/documents/{documentId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-04 : Mettre à jour une pièce KYC")
    public CrmKycDocumentData update(
            @PathParam("documentId") Long documentId,
            CrmKycDocumentData dto) {
        return writeService.updateDocument(documentId, dto);
    }

    // ─── Suppression ─────────────────────────────────────────────────────────

    @DELETE
    @Path("/documents/{documentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-04 : Supprimer une pièce KYC")
    public void delete(@PathParam("documentId") Long documentId) {
        writeService.deleteDocument(documentId);
    }

    // ─── Vérification / LBC-FT (SF-01-06) ───────────────────────────────────

    @POST
    @Path("/documents/{documentId}/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-06 : Marquer une pièce comme vérifiée avec niveau de risque LBC/FT")
    public CrmKycDocumentData verify(
            @PathParam("documentId") Long documentId,
            CrmKycDocumentData dto) {
        return writeService.verifyDocument(
                documentId,
                dto.getVerifiedByUserId(),
                dto.getRiskLevelEnum(),
                dto.getVerificationNotes());
    }

    // ─── Job : marquer les pièces expirées (SF-01-05) ───────────────────────

    @POST
    @Path("/documents/mark-expired")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "SF-01-05 : Mettre à jour le statut des pièces expirées (job schedulé)")
    public String markExpired() {
        int count = writeService.markExpiredDocuments();
        return "{\"markedExpired\":" + count + "}";
    }
}
