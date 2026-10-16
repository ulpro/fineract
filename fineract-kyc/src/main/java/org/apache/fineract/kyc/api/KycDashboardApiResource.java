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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.kyc.data.KycDashboardStatsData;
import org.apache.fineract.kyc.service.KycDashboardReadPlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/kyc/dashboard")
@Component
@Tag(name = "KYC Dashboard", description = "Operations for KYC Dashboard Statistics")
@RequiredArgsConstructor
public class KycDashboardApiResource {

    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<KycDashboardStatsData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final KycDashboardReadPlatformService readPlatformService;

    @GET
    @Path("stats")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveStats(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(KycApiConstants.KYC_CASE_RESOURCE_NAME);

        final KycDashboardStatsData statsData = this.readPlatformService.retrieveStats();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, statsData);
    }
}
