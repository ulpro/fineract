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
import jakarta.ws.rs.Path;
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
import org.apache.fineract.kyc.data.KycDocumentTypeData;
import org.apache.fineract.kyc.service.KycDocumentTypeReadPlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/kyc/document-types")
@Component
@Tag(name = "KYC Document Types", description = "Manage the reference list of document types accepted for KYC verification.")
@RequiredArgsConstructor
public class KycDocumentTypeApiResource {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = KycApiConstants.KYC_DOCUMENT_TYPE_RESOURCE_NAME;

    private final PlatformSecurityContext context;
    private final KycDocumentTypeReadPlatformService readPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<KycDocumentTypeData> toApiJsonSerializer;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List KYC Document Types", description = "Returns all active and inactive KYC document types.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = KycDocumentTypeData.class)))) })
    public List<KycDocumentTypeData> retrieveAll() {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        return this.readPlatformService.retrieveAll();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a KYC Document Type", description = "Creates a new KYC document type reference entry.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = KycDocumentTypeData.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CommandProcessingResult.class))) })
    public CommandProcessingResult create(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createKycDocumentType().withJson(apiRequestBodyAsJson).build();
        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }
}
