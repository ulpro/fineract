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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.springframework.stereotype.Component;

@Path("/v1/kyc/documents")
@Component
@Tag(name = "KYC Document Workflow", description = "Manage Maker Checker approval workflow for KYC documents.")
@RequiredArgsConstructor
public class KycDocumentWorkflowApiResource {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @POST
    @Path("{documentId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Perform Workflow Action on a KYC Document", description = "Allows submitting, approving, or rejecting a KYC Document.")
    @RequestBody(required = false, content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CommandProcessingResult.class))) })
    public CommandProcessingResult executeWorkflowAction(
            @PathParam("documentId") @Parameter(description = "documentId") final Long documentId,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

        CommandWrapper commandRequest = null;
        if (is(commandParam, "submit")) {
            commandRequest = builder.submitKycDocument(documentId).build();
        } else if (is(commandParam, "approve")) {
            commandRequest = builder.approveKycDocument(documentId).build();
        } else if (is(commandParam, "reject")) {
            commandRequest = builder.rejectKycDocument(documentId).build();
        }

        if (commandRequest == null) {
            throw new UnrecognizedQueryParamException("command", commandParam, "submit", "approve", "reject");
        }

        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
