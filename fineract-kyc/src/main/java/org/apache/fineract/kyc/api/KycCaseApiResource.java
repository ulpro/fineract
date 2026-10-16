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

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.kyc.data.KycAuditLogData;
import org.apache.fineract.kyc.data.KycCaseData;
import org.apache.fineract.kyc.data.KycCaseHistoryData;
import org.apache.fineract.kyc.service.KycCaseReadPlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/kyc/cases")
@Component
@Tag(name = "KYC Case", description = "Operations for KYC Case Management")
@RequiredArgsConstructor
public class KycCaseApiResource {

    private final Set<String> responseDataParameters = new HashSet<>(
            Arrays.asList("id", "clientId", "clientName", "tier", "workflowStatusId", "workflowStatusCode", "workflowStatusValue",
                    "assignedAgentId", "assignedAgentName", "assignedDate", "assignmentStatusId", "assignmentStatusCode",
                    "assignmentStatusValue", "riskLevelId", "riskLevelCode", "riskLevelValue", "riskScore", "riskReason", "riskFlags",
                    "escalationReason", "complianceNotes", "documents", "requiredDocumentCount", "submittedDocumentCount",
                    "completionPercentage", "createdDate", "createdByUsername", "lastModifiedDate", "lastModifiedByUsername"));

    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<KycCaseData> toApiJsonSerializer;
    private final DefaultToApiJsonSerializer<KycCaseHistoryData> historyToApiJsonSerializer;
    private final DefaultToApiJsonSerializer<KycAuditLogData> auditToApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final KycCaseReadPlatformService readPlatformService;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAllCases(@Context final UriInfo uriInfo, @QueryParam("tierId") final Long tierId,
            @QueryParam("statusId") final Integer statusId, @QueryParam("riskLevelId") final Integer riskLevelId,
            @QueryParam("assignmentStatusId") final Integer assignmentStatusId, @QueryParam("offset") final Integer offset,
            @QueryParam("limit") final Integer limit, @QueryParam("orderBy") final String orderBy,
            @QueryParam("sortOrder") final String sortOrder, @QueryParam("searchQuery") final String searchQuery) {

        this.context.authenticatedUser().validateHasReadPermission(KycApiConstants.KYC_CASE_RESOURCE_NAME);

        final SearchParameters searchParameters = SearchParameters.builder().limit(limit).offset(offset).orderBy(orderBy)
                .sortOrder(sortOrder).name(searchQuery).build();

        final Page<KycCaseData> caseData = this.readPlatformService.retrieveAllCases(searchParameters, tierId, statusId, riskLevelId,
                assignmentStatusId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, caseData, this.responseDataParameters);
    }

    @GET
    @Path("{caseId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveCase(@PathParam("caseId") final Long caseId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(KycApiConstants.KYC_CASE_RESOURCE_NAME);

        final KycCaseData caseData = this.readPlatformService.retrieveOne(caseId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, caseData, this.responseDataParameters);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createCase(final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createKycCase().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("{caseId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String transitionWorkflow(@PathParam("caseId") final Long caseId, @QueryParam("command") final String commandParam,
            final String apiRequestBodyAsJson) {

        CommandWrapper commandRequest = null;
        if (is(commandParam, "transition")) {
            commandRequest = new CommandWrapperBuilder().transitionKycCaseWorkflow(caseId).withJson(apiRequestBodyAsJson).build();
        }

        if (commandRequest == null) {
            // throw exception
        }

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("{caseId}/assign")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String assignAgent(@PathParam("caseId") final Long caseId, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().assignKycCase(caseId).withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{caseId}/risk")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateRisk(@PathParam("caseId") final Long caseId, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateKycCaseRisk(caseId).withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{caseId}/edd")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateEdd(@PathParam("caseId") final Long caseId, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateKycCaseEdd(caseId).withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("{caseId}/flags")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String addFlag(@PathParam("caseId") final Long caseId, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().addKycCaseFlag(caseId).withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{caseId}/flags/{flagId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String resolveFlag(@PathParam("caseId") final Long caseId, @PathParam("flagId") final Long flagId,
            final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().resolveKycCaseFlag(caseId, flagId).withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("{caseId}/history")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveHistory(@PathParam("caseId") final Long caseId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(KycApiConstants.KYC_CASE_RESOURCE_NAME);

        final List<KycCaseHistoryData> historyData = this.readPlatformService.retrieveHistory(caseId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.historyToApiJsonSerializer.serialize(settings, historyData);
    }

    @GET
    @Path("{caseId}/audit")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAuditLogs(@PathParam("caseId") final Long caseId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(KycApiConstants.KYC_CASE_RESOURCE_NAME);

        final List<KycAuditLogData> auditData = this.readPlatformService.retrieveAuditLogs(caseId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.auditToApiJsonSerializer.serialize(settings, auditData);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return org.apache.commons.lang3.StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
