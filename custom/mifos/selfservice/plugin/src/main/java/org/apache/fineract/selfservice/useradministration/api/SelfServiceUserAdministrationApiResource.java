/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fineract.selfservice.useradministration.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserClientMapping;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserClientMappingRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.stereotype.Component;

@Path("/v1/selfserviceusers")
@Component
@Tag(name = "Self Service User Administration", description = "Back-office API for managing self-service users.")
@RequiredArgsConstructor
public class SelfServiceUserAdministrationApiResource {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final AppSelfServiceUserClientMappingRepository clientMappingRepository;
    private final ClientRepositoryWrapper clientRepository;
    private final DefaultToApiJsonSerializer<Map<String, Object>> mapSerializer;
    private final DefaultToApiJsonSerializer<CommandProcessingResult> resultSerializer;

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String create(final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .withEntityName("SELFSERVICEUSER")
                .withActionName("CREATE")
                .withHref("/selfserviceusers")
                .withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.resultSerializer.serialize(result);
    }

    @GET
    @Path("client/{clientId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getSelfServiceUserForClient(@PathParam("clientId") final Long clientId) {
        // Critique #2 & #3: Permission check & Hierarchy validation
        final AppUser currentUser = this.context.authenticatedUser();
        currentUser.validateHasPermissionTo("READ_SELFSERVICEUSER");

        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
        final String clientHierarchy = client.getOffice().getHierarchy();
        final String userHierarchy = currentUser.getOffice().getHierarchy();
        if (!clientHierarchy.startsWith(userHierarchy)) {
            throw new ClientNotFoundException(clientId);
        }

        final AppSelfServiceUserClientMapping mapping = this.clientMappingRepository.fetchByClientId(clientId);
        final Map<String, Object> response = new HashMap<>();

        if (mapping != null && mapping.getAppUser() != null) {
            final AppSelfServiceUser user = mapping.getAppUser();
            response.put("exists", true);
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("active", user.isEnabled());
        } else {
            response.put("exists", false);
        }

        return this.mapSerializer.serialize(response);
    }

    @PUT
    @Path("{userId}/status")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateStatus(@PathParam("userId") final Long userId, final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .withEntityName("SELFSERVICEUSER")
                .withActionName("UPDATE")
                .withEntityId(userId)
                .withHref("/selfserviceusers/" + userId + "/status")
                .withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.resultSerializer.serialize(result);
    }
}
