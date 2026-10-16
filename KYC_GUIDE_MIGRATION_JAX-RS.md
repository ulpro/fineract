# GUIDE DE MIGRATION KYC → JAX-RS (Conforme Apache Fineract)

## 🎯 Objectif

Convertir les 3 classes API Spring MVC du module KYC en classes JAX-RS conformes à Apache Fineract.

---

## 📋 CLASSES À MIGRER

| Classe | Chemin | Status |
|--------|--------|--------|
| KycProfileApi | `fineract-kyc/src/main/java/org/apache/fineract/portfolio/kyc/api/KycProfileApi.java` | 🔴 À Migrer |
| KycDocumentApi | `fineract-kyc/src/main/java/org/apache/fineract/portfolio/kyc/api/KycDocumentApi.java` | 🔴 À Migrer |
| KycWorkflowApi | `fineract-kyc/src/main/java/org/apache/fineract/portfolio/kyc/api/KycWorkflowApi.java` | 🔴 À Migrer |
| KycAuditLogApi | `fineract-kyc/src/main/java/org/apache/fineract/portfolio/kyc/api/KycAuditLogApi.java` | 🟢 Déjà Conforme |

---

## 🔄 MIGRATION 1: KycProfileApi

### AVANT (Spring MVC - ❌ Non-Conforme)

```java
package org.apache.fineract.portfolio.kyc.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;

@Slf4j
@RestController
@RequestMapping("/api/kyc/profiles")
@RequiredArgsConstructor
@Tag(name = "KYC Profile Management")
public class KycProfileApi {
    private final KycProfileService kycProfileService;

    @PostMapping
    @Operation(summary = "Initiate KYC Profile")
    public ResponseEntity<ApiResponse<KycProfileResponse>> createProfile(
            @Valid @RequestBody CreateKycProfileRequest request) {
        KycProfileResponse response = kycProfileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "..."));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get KYC Profile by ID")
    public ResponseEntity<ApiResponse<KycProfileResponse>> getProfileById(
            @PathVariable UUID id) {
        KycProfileResponse response = kycProfileService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "..."));
    }
}
```

### APRÈS (JAX-RS - ✅ Conforme)

```java
package org.apache.fineract.portfolio.kyc.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.kyc.data.CreateKycProfileRequest;
import org.apache.fineract.portfolio.kyc.data.KycProfileResponse;
import org.apache.fineract.portfolio.kyc.data.PagedResponse;
import org.apache.fineract.portfolio.kyc.service.KycProfileService;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
@Path("/v1/kyc/profiles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Tag(name = "KYC Profile Management", description = "REST APIs for managing KYC Profiles")
public class KycProfileApi {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "KYC_PROFILE";

    private final PlatformSecurityContext securityContext;
    private final KycProfileService kycProfileService;
    private final DefaultToApiJsonSerializer<KycProfileResponse> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    // POST /v1/kyc/profiles
    @POST
    @Operation(summary = "Initiate KYC Profile",
               description = "Creates a new KYC Profile aggregate linked to a Fineract Client ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "KYC Profile successfully initiated",
                    content = @Content(schema = @Schema(implementation = KycProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @ApiResponse(responseCode = "409", description = "KYC Profile already exists for this Client", content = @Content)
    })
    public CommandProcessingResult createProfile(
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        log.info("REST request to initiate KYC profile");

        this.securityContext.authenticatedUser().validateHasCreatePermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .createKycProfile()
                .withJson(apiRequestBodyAsJson)
                .build();

        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    // GET /v1/kyc/profiles/{id}
    @GET
    @Path("{id}")
    @Operation(summary = "Get KYC Profile by ID",
               description = "Retrieves a single KYC Profile by its UUID primary key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the KYC Profile",
                    content = @Content(schema = @Schema(implementation = KycProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "KYC Profile not found", content = @Content)
    })
    public String getProfileById(
            @PathParam("id") @Parameter(description = "UUID of the KYC profile") final UUID id,
            @Context final UriInfo uriInfo) {
        log.info("REST request to get KYC profile by ID: {}", id);

        this.securityContext.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final KycProfileResponse response = this.kycProfileService.getProfileById(id);
        final ApiRequestJsonSerializationSettings settings =
            this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, response);
    }

    // GET /v1/kyc/profiles/client/{clientId}
    @GET
    @Path("client/{clientId}")
    @Operation(summary = "Get KYC Profile by Client ID",
               description = "Retrieves a KYC Profile by the associated Apache Fineract Client ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the KYC Profile",
                    content = @Content(schema = @Schema(implementation = KycProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "KYC Profile not found for the client", content = @Content)
    })
    public String getProfileByClientId(
            @PathParam("clientId") @Parameter(description = "Fineract client reference ID") final Long clientId,
            @Context final UriInfo uriInfo) {
        log.info("REST request to get KYC profile by client ID: {}", clientId);

        this.securityContext.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final KycProfileResponse response = this.kycProfileService.getProfileByClientId(clientId);
        final ApiRequestJsonSerializationSettings settings =
            this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, response);
    }

    // PUT /v1/kyc/profiles/{id}
    @PUT
    @Path("{id}")
    @Operation(summary = "Update KYC Profile",
               description = "Direct CRUD update for a KYC profile's risk level, risk score, and comments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC Profile fields successfully updated",
                    content = @Content(schema = @Schema(implementation = KycProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed / Bad Request", content = @Content),
            @ApiResponse(responseCode = "404", description = "KYC Profile not found", content = @Content)
    })
    public CommandProcessingResult updateProfile(
            @PathParam("id") @Parameter(description = "UUID of the KYC profile") final UUID id,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        log.info("REST request to update KYC profile ID: {}", id);

        this.securityContext.authenticatedUser().validateHasEditPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .updateKycProfile(id)
                .withJson(apiRequestBodyAsJson)
                .build();

        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    // GET /v1/kyc/profiles (List avec filtres)
    @GET
    @Operation(summary = "List and Filter KYC Profiles",
               description = "Returns a paginated list of KYC Profiles. Supports filtering by status and risk level.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profiles retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    public String getAllProfiles(
            @QueryParam("status") @Parameter(description = "Optional filter by KycStatus") final String status,
            @QueryParam("riskLevel") @Parameter(description = "Optional filter by RiskLevel") final String riskLevel,
            @QueryParam("offset") @DefaultValue("0") @Parameter(description = "Offset for pagination") final Integer offset,
            @QueryParam("limit") @DefaultValue("50") @Parameter(description = "Limit for pagination") final Integer limit,
            @QueryParam("orderBy") @Parameter(description = "Order by field") final String orderBy,
            @QueryParam("sortOrder") @Parameter(description = "Sort order (ASC/DESC)") final String sortOrder,
            @Context final UriInfo uriInfo) {
        log.info("REST request to list and filter KYC profiles");

        this.securityContext.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        // Construire les paramètres de recherche
        final SearchParameters searchParameters = SearchParameters.builder()
                .offset(offset)
                .limit(limit)
                .orderBy(orderBy)
                .sortOrder(sortOrder)
                .build();

        final PagedResponse<KycProfileResponse> responsePage =
            this.kycProfileService.getAllProfiles(status, riskLevel, searchParameters);

        final ApiRequestJsonSerializationSettings settings =
            this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, responsePage);
    }
}
```

### Changements Clés

| Aspect | Avant | Après |
|--------|-------|-------|
| **Classe** | `@RestController` | `@Component` |
| **Path** | `@RequestMapping("/api/kyc/profiles")` | `@Path("/v1/kyc/profiles")` |
| **Content** | Implicite | `@Consumes`, `@Produces` |
| **GET** | `@GetMapping("/{id}")` | `@GET` + `@Path("{id}")` |
| **POST** | `@PostMapping` | `@POST` |
| **PUT** | `@PutMapping` | `@PUT` |
| **Paramètres** | `@PathVariable UUID id` | `@PathParam("id") final UUID id` |
| **Query params** | `@RequestParam` | `@QueryParam` + `@DefaultValue` |
| **Request body** | `@RequestBody CreateKycProfileRequest` | `@Parameter(hidden = true) final String apiRequestBodyAsJson` |
| **Return** | `ResponseEntity<ApiResponse<?>>` | `CommandProcessingResult` (write) ou `String` (read) |
| **Context** | N/A | `@Context UriInfo uriInfo` |
| **Permissions** | N/A | `securityContext.authenticatedUser().validate...()` |

---

## 🔄 MIGRATION 2: KycDocumentApi

### AVANT (Spring - ❌)

```java
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycDocumentApi {

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> registerDocument(
            @Valid @RequestBody KycDocumentRequest request) { ... }

    @GetMapping("/profiles/{profileId}/documents")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> getProfileDocuments(
            @PathVariable UUID profileId) { ... }

    @PutMapping("/documents/{id}/review")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> reviewDocument(
            @PathVariable UUID id,
            @RequestParam DocumentStatus status,
            @RequestParam(required = false) String comment) { ... }
}
```

### APRÈS (JAX-RS - ✅)

```java
package org.apache.fineract.portfolio.kyc.api;

import io.swagger.v3.oas.annotations.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.kyc.service.KycDocumentService;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
@Path("/v1/kyc/documents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Tag(name = "KYC Document Management", description = "REST APIs for managing KYC documents")
public class KycDocumentApi {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "KYC_DOCUMENT";

    private final PlatformSecurityContext securityContext;
    private final KycDocumentService kycDocumentService;
    private final DefaultToApiJsonSerializer<?> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    // POST /v1/kyc/documents
    @POST
    @Operation(summary = "Register KYC Document Metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Document registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public CommandProcessingResult registerDocument(
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        log.info("REST request to register KYC document");
        this.securityContext.authenticatedUser().validateHasCreatePermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .createKycDocument()
                .withJson(apiRequestBodyAsJson)
                .build();

        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    // GET /v1/kyc/documents/profiles/{profileId}
    @GET
    @Path("profiles/{profileId}")
    @Operation(summary = "List KYC Documents for Profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents listed successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public String getProfileDocuments(
            @PathParam("profileId") @Parameter(description = "UUID of the KYC profile") final UUID profileId,
            @Context final UriInfo uriInfo) {
        log.info("REST request to list documents for profile ID: {}", profileId);
        this.securityContext.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final Object response = this.kycDocumentService.getProfileDocuments(profileId);
        final ApiRequestJsonSerializationSettings settings =
            this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, response);
    }

    // PUT /v1/kyc/documents/{id}/review
    @PUT
    @Path("{id}/review")
    @Operation(summary = "Review KYC Document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document reviewed successfully"),
            @ApiResponse(responseCode = "400", description = "Document not in pending state"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public CommandProcessingResult reviewDocument(
            @PathParam("id") @Parameter(description = "UUID of the document") final UUID id,
            @QueryParam("status") @Parameter(description = "Review outcome (APPROVED/REJECTED)") final String status,
            @QueryParam("comment") @Parameter(description = "Agent justification") final String comment) {
        log.info("REST request to review document ID: {} as {}", id, status);
        this.securityContext.authenticatedUser().validateHasEditPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        // Construire le JSON payload pour la requête
        final String payload = constructReviewPayload(status, comment);

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .reviewKycDocument(id)
                .withJson(payload)
                .build();

        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private String constructReviewPayload(final String status, final String comment) {
        // Construire JSON manuellement ou utiliser JsonBuilder
        return String.format("{\"status\":\"%s\", \"comment\":\"%s\"}", status, comment);
    }
}
```

---

## 🔄 MIGRATION 3: KycWorkflowApi

### AVANT (Spring - ❌)

```java
@RestController
@RequestMapping("/api/kyc/profiles")
public class KycWorkflowApi {

    @PostMapping("/{id}/transitions")
    public ResponseEntity<ApiResponse<KycProfileResponse>> triggerTransition(
            @PathVariable UUID id,
            @RequestParam KycStatus targetStatus,
            @RequestParam(required = false) String comment) { ... }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<KycWorkflowHistoryResponse>>> getProfileHistory(
            @PathVariable UUID id) { ... }
}
```

### APRÈS (JAX-RS - ✅)

```java
package org.apache.fineract.portfolio.kyc.api;

import io.swagger.v3.oas.annotations.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.kyc.service.KycWorkflowService;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
@Path("/v1/kyc/profiles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Tag(name = "KYC Workflow Engine", description = "REST APIs for executing and querying KYC state transitions")
public class KycWorkflowApi {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "KYC_WORKFLOW";

    private final PlatformSecurityContext securityContext;
    private final KycWorkflowService kycWorkflowService;
    private final DefaultToApiJsonSerializer<?> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    // POST /v1/kyc/profiles/{id}/transitions
    @POST
    @Path("{id}/transitions")
    @Operation(summary = "Trigger KYC Status Transition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transition successful"),
            @ApiResponse(responseCode = "400", description = "Invalid transition / rule violation"),
            @ApiResponse(responseCode = "404", description = "KYC Profile not found")
    })
    public CommandProcessingResult triggerTransition(
            @PathParam("id") @Parameter(description = "UUID of the KYC profile") final UUID id,
            @QueryParam("targetStatus") @Parameter(description = "Target status to transition to") final String targetStatus,
            @QueryParam("comment") @Parameter(description = "Comment detailing the reason") final String comment) {
        log.info("REST request to transition KYC profile ID: {} to {}", id, targetStatus);
        this.securityContext.authenticatedUser().validateHasEditPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        // Construire le JSON payload
        final String payload = constructTransitionPayload(targetStatus, comment);

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .transitionKycProfile(id)
                .withJson(payload)
                .build();

        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    // GET /v1/kyc/profiles/{id}/history
    @GET
    @Path("{id}/history")
    @Operation(summary = "Get Profile Workflow History Timeline")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "KYC Profile not found")
    })
    public String getProfileHistory(
            @PathParam("id") @Parameter(description = "UUID of the KYC profile") final UUID id,
            @Context final UriInfo uriInfo) {
        log.info("REST request to get workflow history for profile ID: {}", id);
        this.securityContext.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final Object response = this.kycWorkflowService.getProfileHistory(id);
        final ApiRequestJsonSerializationSettings settings =
            this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, response);
    }

    private String constructTransitionPayload(final String targetStatus, final String comment) {
        return String.format("{\"targetStatus\":\"%s\", \"comment\":\"%s\"}", targetStatus, comment);
    }
}
```

---

## 📝 ÉTAPES D'IMPLÉMENTATION

### Étape 1: Mise à jour des imports (Toutes les classes)

```java
// SUPPRIMER
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

// AJOUTER
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.stereotype.Component;
```

### Étape 2: Mise à jour des annotations de classe

```java
// AVANT
@RestController
@RequestMapping("/api/kyc/...")
@RequiredArgsConstructor
public class KycProfileApi { }

// APRÈS
@Component
@Path("/v1/kyc/...")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class KycProfileApi { }
```

### Étape 3: Injection des dépendances Fineract

```java
// AJOUTER à tous les constructeurs
private final PlatformSecurityContext securityContext;
private final DefaultToApiJsonSerializer<?> toApiJsonSerializer;
private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
private final ApiRequestParameterHelper apiRequestParameterHelper;

// Ou utiliser @RequiredArgsConstructor avec champs final
@RequiredArgsConstructor
public class KycProfileApi {
    private final PlatformSecurityContext securityContext;
    private final DefaultToApiJsonSerializer<?> toApiJsonSerializer;
    // ...
}
```

### Étape 4: Conversion des méthodes (Par exemple POST)

```java
// AVANT
@PostMapping
public ResponseEntity<ApiResponse<KycProfileResponse>> createProfile(
        @Valid @RequestBody CreateKycProfileRequest request) {
    KycProfileResponse response = kycProfileService.createProfile(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(response, "..."));
}

// APRÈS
@POST
@Operation(summary = "...", description = "...")
@ApiResponses(value = { @ApiResponse(...), ... })
public CommandProcessingResult createProfile(
        @Parameter(hidden = true) final String apiRequestBodyAsJson) {
    this.securityContext.authenticatedUser().validateHasCreatePermission(RESOURCE_NAME_FOR_PERMISSIONS);

    final CommandWrapper commandRequest = new CommandWrapperBuilder()
            .createKycProfile()
            .withJson(apiRequestBodyAsJson)
            .build();

    return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
}
```

### Étape 5: Conversion des méthodes GET

```java
// AVANT
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<KycProfileResponse>> getProfileById(
        @PathVariable UUID id) {
    KycProfileResponse response = kycProfileService.getProfileById(id);
    return ResponseEntity.ok(ApiResponse.ok(response, "..."));
}

// APRÈS
@GET
@Path("{id}")
@Operation(summary = "...", description = "...")
public String getProfileById(
        @PathParam("id") @Parameter(description = "...") final UUID id,
        @Context final UriInfo uriInfo) {
    this.securityContext.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

    final KycProfileResponse response = this.kycProfileService.getProfileById(id);
    final ApiRequestJsonSerializationSettings settings =
        this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

    return this.toApiJsonSerializer.serialize(settings, response);
}
```

---

## ✅ CHECKLIST DE VALIDATION

**Pour chaque classe migrée**:

- [ ] Annotations de classe mises à jour (@Component, @Path, @Consumes, @Produces)
- [ ] Tous les imports Spring Web supprimés
- [ ] Tous les imports JAX-RS ajoutés
- [ ] Dépendances Fineract injectées (securityContext, toApiJsonSerializer, etc.)
- [ ] Toutes les méthodes utilisant @GET, @POST, @PUT, @DELETE (pas @GetMapping, etc.)
- [ ] Toutes les méthodes utilisant @PathParam/@QueryParam (pas @PathVariable, @RequestParam)
- [ ] Permissions validées (securityContext.authenticatedUser().validate...)
- [ ] Retours ajustés (String JSON ou CommandProcessingResult)
- [ ] @Context UriInfo injecté pour les GET
- [ ] Annotations Swagger (@Operation, @ApiResponse) ajoutées
- [ ] Routes compatibles avec JerseyConfig (@Path détecté)

---

## 🧪 TEST DE VÉRIFICATION

Après migration, vérifier:

```bash
# 1. Les classes sont détectées par Spring
@Component → Vérifier dans Spring Boot logs

# 2. Les classes sont enregistrées par Jersey
# Chercher dans JerseyConfig.setup(): les beans @Path

# 3. API accessible
curl http://localhost:8080/api/v1/kyc/profiles

# 4. Swagger OpenAPI généré
curl http://localhost:8080/v3/api-docs | grep "kyc/profiles"

# 5. Pas de conflits Spring DispatcherServlet
# (Vérifier logs pour routing ambigus)
```

---

## 📊 RÉSULTAT ATTENDU

**Avant Migration**:
```
Jersey ResourceConfig
├─ KycProfileApi        ❌ Pas détecté (@RestController, pas @Path)
├─ KycDocumentApi       ❌ Pas détecté (@RestController, pas @Path)
├─ KycWorkflowApi       ❌ Pas détecté (@RestController, pas @Path)
└─ KycAuditLogApi       ✅ Détecté (@Path)

Spring DispatcherServlet
├─ /api/kyc/profiles    → KycProfileApi
├─ /api/kyc             → KycDocumentApi
└─ /api/kyc/profiles    → KycWorkflowApi (conflit ?)
```

**Après Migration**:
```
Jersey ResourceConfig
├─ KycProfileApi        ✅ Détecté (@Path("/v1/kyc/profiles"))
├─ KycDocumentApi       ✅ Détecté (@Path("/v1/kyc/documents"))
├─ KycWorkflowApi       ✅ Détecté (@Path("/v1/kyc/profiles"))
└─ KycAuditLogApi       ✅ Détecté (@Path("/v1/kyc/audit"))

Spring DispatcherServlet
└─ (Plus aucune route /api/kyc/*)
```

**Exposition Unifié**:
```
✓ Tous les endpoints sous /api/v1/kyc/*
✓ Jersey détecte et expose tous les endpoints
✓ Swagger OpenAPI généré de manière cohérente
✓ Conforme avec autres modules Fineract
```
