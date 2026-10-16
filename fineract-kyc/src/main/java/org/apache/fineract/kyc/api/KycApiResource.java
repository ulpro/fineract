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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Path("/v1/kyc")
@Component
@Tag(name = "KYC", description = "Know Your Customer (KYC) Module APIs")
public class KycApiResource {

    @GET
    @Path("/ping")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Check KYC Module Status", description = "Returns the status of the KYC module to verify it is loaded correctly.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK") })
    public Map<String, String> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("module", "kyc");
        response.put("status", "UP");
        return response;
    }
}
