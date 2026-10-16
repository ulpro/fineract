package org.apache.fineract.accounting.cemac.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collection;
import org.apache.fineract.accounting.cemac.data.CemacCodeData;
import org.apache.fineract.accounting.cemac.service.CemacCodeReadPlatformService;
import org.apache.fineract.accounting.cemac.service.CemacCodeWritePlatformService;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/v1/cemaccodes")
@Component
@Scope("singleton")
@Tag(name = "CEMAC Codes", description = "Administer CEMAC / COBAC Codes mapped to PCEMF")
public class CemacCodesApiResource {

    private final PlatformSecurityContext context;
    private final CemacCodeReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<CemacCodeData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CemacCodeWritePlatformService writePlatformService;

    @Autowired
    public CemacCodesApiResource(final PlatformSecurityContext context, final CemacCodeReadPlatformService readPlatformService,
            final DefaultToApiJsonSerializer<CemacCodeData> toApiJsonSerializer, final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final CemacCodeWritePlatformService writePlatformService) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.writePlatformService = writePlatformService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAllCemacCodes(@Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission("CEMACCODE");

        final Collection<CemacCodeData> cemacCodes = this.readPlatformService.retrieveAllCemacCodes();
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, cemacCodes);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createCemacCode(final String apiRequestBodyAsJson) {
        this.context.authenticatedUser().validateHasPermissionTo("CREATE_CEMACCODE");

        // We bypass standard CommandWrapper architecture for simplicity, or we can use the injected service directly.
        // Let's use the explicit service call for direct handling.
        final CommandWrapper commandRequest = new CommandWrapperBuilder().withEntityName("CEMACCODE").withActionName("CREATE").withHref("/cemaccodes").withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{cemacCodeId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteCemacCode(@PathParam("cemacCodeId") final Long cemacCodeId) {
        this.context.authenticatedUser().validateHasPermissionTo("DELETE_CEMACCODE");

        final CommandWrapper commandRequest = new CommandWrapperBuilder().withEntityName("CEMACCODE").withActionName("DELETE").withHref("/cemaccodes/" + cemacCodeId).withEntityId(cemacCodeId).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }
}
