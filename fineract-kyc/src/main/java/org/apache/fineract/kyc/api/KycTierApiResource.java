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
package org.apache.fineract.kyc.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.kyc.data.KycTierData;
import org.apache.fineract.kyc.service.KycTierReadPlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/kyc/tiers")
@Component
@Tag(name = "KYC Tiers", description = "Manage KYC compliance tiers and their required document types.")
@RequiredArgsConstructor
public class KycTierApiResource {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = KycApiConstants.KYC_TIER_RESOURCE_NAME;

    private final PlatformSecurityContext context;
    private final KycTierReadPlatformService readPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<KycTierData> toApiJsonSerializer;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List KYC Tiers", description = "Returns all KYC tiers ordered by level ascending.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = KycTierData.class)))) })
    public List<KycTierData> retrieveAll() {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        return this.readPlatformService.retrieveAll();
    }

    @GET
    @Path("{tierId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a KYC Tier", description = "Returns a single KYC tier by its identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = KycTierData.class))) })
    public KycTierData retrieveOne(@PathParam("tierId") @Parameter(description = "tierId") final Long tierId) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        return this.readPlatformService.retrieveOne(tierId);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a KYC Tier", description = "Creates a new KYC compliance tier with optional document requirements.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = KycTierData.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CommandProcessingResult.class))) })
    public CommandProcessingResult create(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createKycTier().withJson(apiRequestBodyAsJson).build();
        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @PUT
    @Path("{tierId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update a KYC Tier", description = "Updates an existing KYC tier. Document requirements are replaced in full if provided.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = KycTierData.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CommandProcessingResult.class))) })
    public CommandProcessingResult update(@PathParam("tierId") @Parameter(description = "tierId") final Long tierId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateKycTier(tierId).withJson(apiRequestBodyAsJson).build();
        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }
}
