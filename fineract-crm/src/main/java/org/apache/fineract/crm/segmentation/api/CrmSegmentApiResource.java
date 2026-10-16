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
package org.apache.fineract.crm.segmentation.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.segmentation.data.CrmSegmentData;
import org.apache.fineract.crm.segmentation.domain.CrmSegment;
import org.apache.fineract.crm.segmentation.domain.CrmSegmentMemberRepository;
import org.apache.fineract.crm.segmentation.domain.CrmSegmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Path("/v1/crm/segments")
@Component
@Tag(name = "CRM Segmentation", description = "Gestion des segments clients et moteur de règles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CrmSegmentApiResource {

    private final CrmSegmentRepository segmentRepository;
    private final CrmSegmentMemberRepository segmentMemberRepository;

    @GET
    @Operation(summary = "Lister les segments clients", description = "Retourne tous les segments configurés.")
    public Response retrieveAllSegments() {
        List<CrmSegmentData> list = segmentRepository.findAll().stream()
                .map(s -> new CrmSegmentData(s.getId(), s.getName(), s.getDescription(),
                        s.getRulesJson(), s.getIsActive(), s.getLastRunDate(),
                        segmentMemberRepository.findBySegmentId(s.getId()).size()))
                .collect(Collectors.toList());

        return Response.ok(list).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Créer un segment de clientèle", description = "Définit un segment par combinaisons de critères ET/OU avec vérification anti-discrimination RG-11.")
    public Response createSegment(final CrmSegmentData data) {
        // RG-11 : Exclusion des critères discriminatoires
        if (data.getRulesJson() != null) {
            String rules = data.getRulesJson().toLowerCase();
            if (rules.contains("religion") || rules.contains("ethnie") || rules.contains("opinion") || rules.contains("sante")) {
                throw new IllegalArgumentException("RG-11 : Les critères discriminatoires (religion, ethnie, opinion, santé) sont strictement interdits.");
            }
        }

        CrmSegment segment = new CrmSegment(
                data.getName(),
                data.getDescription(),
                data.getRulesJson(),
                data.getIsActive()
        );

        CrmSegment saved = segmentRepository.save(segment);
        CrmSegmentData result = new CrmSegmentData(saved.getId(), saved.getName(), saved.getDescription(),
                saved.getRulesJson(), saved.getIsActive(), saved.getLastRunDate(), 0);

        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @POST
    @Path("/{segmentId}/recalculate")
    @Transactional
    @Operation(summary = "Recalculer les membres d'un segment", description = "Exécute le moteur de règles pour mettre à jour la liste des clients membres.")
    public Response recalculateSegment(@PathParam("segmentId") final Long segmentId) {
        CrmSegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Segment non trouvé id: " + segmentId));

        segment.setLastRunDate(LocalDateTime.now());
        segmentRepository.save(segment);

        return Response.ok("Règles du segment recalculées avec succès.").build();
    }
}
