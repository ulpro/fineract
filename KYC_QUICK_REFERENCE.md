# QUICK REFERENCE - Changements Spring → JAX-RS

**Pour convertir rapidement les APIs KYC vers JAX-RS.**

---

## 🔄 CHEAT SHEET: Conversions Directes

### IMPORTS

```java
// ❌ SUPPRIMER
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

// ✅ AJOUTER
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

---

## 📌 ANNOTATIONS DE CLASSE

```diff
- @RestController
- @RequestMapping("/api/kyc/profiles")
+ @Component
+ @Path("/v1/kyc/profiles")
+ @Consumes(MediaType.APPLICATION_JSON)
+ @Produces(MediaType.APPLICATION_JSON)
  @RequiredArgsConstructor
  @Tag(name = "...")
  public class KycProfileApi { }
```

---

## 🔧 CONVERSIONS DE MÉTHODES

### GET (Sans paramètres path)

```diff
- @GetMapping
+ @GET
  public ResponseEntity<ApiResponse<...>> method() {
-     return ResponseEntity.ok(...);
+     return toApiJsonSerializer.serialize(settings, response);
  }
```

### GET (Avec paramètre path)

```diff
- @GetMapping("/{id}")
+ @GET
+ @Path("{id}")
  public ResponseEntity<...> method(
-     @PathVariable UUID id
+     @PathParam("id") final UUID id,
+     @Context final UriInfo uriInfo
  ) { }
```

### GET (Avec paramètres query)

```diff
- @GetMapping
  public ResponseEntity<...> method(
-     @RequestParam(required = false) String status,
+     @QueryParam("status") final String status,
-     @RequestParam @DefaultValue("0") int page,
+     @QueryParam("page") @DefaultValue("0") final int page,
-     @PageableDefault(size = 50) Pageable pageable
+     @QueryParam("limit") @DefaultValue("50") final int limit,
+     @QueryParam("offset") @DefaultValue("0") final int offset
  ) { }
```

### POST

```diff
- @PostMapping
+ @POST
  public ResponseEntity<ApiResponse<...>> method(
-     @Valid @RequestBody CreateRequest request
+     @Parameter(hidden = true) final String apiRequestBodyAsJson
  ) {
-     return ResponseEntity.status(HttpStatus.CREATED).body(...);
+     this.securityContext.authenticatedUser().validateHasCreatePermission(RESOURCE_NAME);
+     final CommandWrapper cmd = new CommandWrapperBuilder()
+         .createResource()
+         .withJson(apiRequestBodyAsJson)
+         .build();
+     return commandsSourceWritePlatformService.logCommandSource(cmd);
  }
```

### PUT

```diff
- @PutMapping("/{id}")
+ @PUT
+ @Path("{id}")
  public ResponseEntity<ApiResponse<...>> method(
-     @PathVariable UUID id,
-     @RequestBody UpdateRequest request
+     @PathParam("id") final UUID id,
+     @Parameter(hidden = true) final String apiRequestBodyAsJson
  ) {
-     return ResponseEntity.ok(...);
+     this.securityContext.authenticatedUser().validateHasEditPermission(RESOURCE_NAME);
+     final CommandWrapper cmd = new CommandWrapperBuilder()
+         .updateResource(id)
+         .withJson(apiRequestBodyAsJson)
+         .build();
+     return commandsSourceWritePlatformService.logCommandSource(cmd);
  }
```

### DELETE

```diff
- @DeleteMapping("/{id}")
+ @DELETE
+ @Path("{id}")
  public ResponseEntity<?> method(
-     @PathVariable UUID id
+     @PathParam("id") final UUID id
  ) {
-     return ResponseEntity.ok(...);
+     this.securityContext.authenticatedUser().validateHasDeletePermission(RESOURCE_NAME);
+     final CommandWrapper cmd = new CommandWrapperBuilder()
+         .deleteResource(id)
+         .build();
+     return commandsSourceWritePlatformService.logCommandSource(cmd);
  }
```

---

## 🎯 CONVERSIONS DE PARAMÈTRES

| Spring | JAX-RS |
|--------|--------|
| `@PathVariable UUID id` | `@PathParam("id") final UUID id` |
| `@PathVariable Long clientId` | `@PathParam("clientId") final Long clientId` |
| `@RequestParam String status` | `@QueryParam("status") final String status` |
| `@RequestParam(required=false) String status` | `@QueryParam("status") final String status` |
| `@RequestParam @DefaultValue("10") int limit` | `@QueryParam("limit") @DefaultValue("10") final int limit` |
| `@RequestBody Request req` | `@Parameter(hidden=true) final String apiRequestBodyAsJson` |
| `Pageable pageable` | `@QueryParam("offset") int offset, @QueryParam("limit") int limit` |

---

## 📤 CONVERSIONS DE RETOURS

### AVANT (Spring)

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<KycProfileResponse>> getProfileById(
        @PathVariable UUID id) {
    KycProfileResponse response = kycProfileService.getProfileById(id);
    return ResponseEntity.ok(ApiResponse.ok(response, "..."));
}

@PostMapping
public ResponseEntity<ApiResponse<KycProfileResponse>> createProfile(
        @Valid @RequestBody CreateKycProfileRequest request) {
    KycProfileResponse response = kycProfileService.createProfile(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(response, "..."));
}
```

### APRÈS (JAX-RS)

```java
@GET
@Path("{id}")
public String getProfileById(
        @PathParam("id") final UUID id,
        @Context final UriInfo uriInfo) {
    KycProfileResponse response = kycProfileService.getProfileById(id);
    final ApiRequestJsonSerializationSettings settings =
        this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
    return this.toApiJsonSerializer.serialize(settings, response);
}

@POST
public CommandProcessingResult createProfile(
        @Parameter(hidden = true) final String apiRequestBodyAsJson) {
    this.securityContext.authenticatedUser().validateHasCreatePermission(RESOURCE_NAME);
    final CommandWrapper commandRequest = new CommandWrapperBuilder()
            .createKycProfile()
            .withJson(apiRequestBodyAsJson)
            .build();
    return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
}
```

---

## 💉 INJECTIONS DE DÉPENDANCES REQUISES

```java
@Component
@Path("/v1/kyc/profiles")
@RequiredArgsConstructor  // ← Génère automatiquement le constructor
public class KycProfileApi {
    // Ces champs DOIVENT être présents
    private final PlatformSecurityContext securityContext;
    private final KycProfileService kycProfileService;
    private final DefaultToApiJsonSerializer<?> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
}
```

---

## ✅ VALIDATIONS DE SÉCURITÉ

```java
// Ajouter au début de chaque méthode

// Pour GET (Read)
this.securityContext.authenticatedUser().validateHasReadPermission(RESOURCE_NAME);

// Pour POST (Create)
this.securityContext.authenticatedUser().validateHasCreatePermission(RESOURCE_NAME);

// Pour PUT (Edit)
this.securityContext.authenticatedUser().validateHasEditPermission(RESOURCE_NAME);

// Pour DELETE
this.securityContext.authenticatedUser().validateHasDeletePermission(RESOURCE_NAME);
```

---

## 📋 CHECKLIST DE VÉRIFICATION

Pour chaque classe convertie:

```
[ ] Classe décorée: @Component + @Path("/v1/...")
[ ] Classe décorée: @Consumes + @Produces
[ ] Tous imports Spring supprimés
[ ] Tous imports JAX-RS ajoutés
[ ] Toutes méthodes: @GET/@POST/@PUT/@DELETE (pas @GetMapping)
[ ] Tous paramètres: @PathParam/@QueryParam (pas @PathVariable/@RequestParam)
[ ] @Context UriInfo injecté dans toutes GET
[ ] @Parameter(hidden=true) String pour request body
[ ] Permissions validées (securityContext.authenticatedUser())
[ ] Retours: String ou CommandProcessingResult
[ ] @Operation + @ApiResponse ajoutées
[ ] Pas d'imports Spring Web
[ ] Format du code correspond à style Fineract
[ ] JerseyConfig détecte la classe (@Path)
```

---

## 🧪 TEST RAPIDE POST-MIGRATION

```bash
# 1. Vérifier classe exposée (logs Spring)
# Chercher: "Registering beans with annotation @Path"

# 2. Vérifier endpoint accessible
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/kyc/profiles

# 3. Vérifier Swagger updated
curl http://localhost:8080/v3/api-docs | grep "kyc"

# 4. Tester requête GET
curl -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/api/v1/kyc/profiles/12345

# 5. Tester requête POST
curl -X POST \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"clientId": 1}' \
     http://localhost:8080/api/v1/kyc/profiles
```

---

## 🚀 ORDRE DE MIGRATION RECOMMANDÉ

1. **Étape 1**: KycProfileApi (classe la plus importante)
2. **Étape 2**: KycDocumentApi (dépend de KycProfileApi)
3. **Étape 3**: KycWorkflowApi (dépend de KycProfileApi)
4. **Étape 4**: Améliorer KycAuditLogApi (ajouter @ApiResponse)

---

## 🔗 RESSOURCES

- Guide complet: `KYC_GUIDE_MIGRATION_JAX-RS.md`
- Analyse détaillée: `KYC_API_CONFORMITE_ANALYSE.md`
- Divergences: `KYC_DIVERGENCES_DETAILLEES.md`

---

## 📞 PATTERNS FINERACT À RESPECTER

### Pattern CommandWrapper (Pour POST/PUT/DELETE)

```java
final CommandWrapper commandRequest = new CommandWrapperBuilder()
    .createKycProfile()          // ou updateKycProfile(id), deleteKycProfile(id)
    .withJson(apiRequestBodyAsJson)
    .build();

return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
```

### Pattern Serialization (Pour GET)

```java
final KycProfileResponse response = this.kycProfileService.getProfileById(id);
final ApiRequestJsonSerializationSettings settings =
    this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
return this.toApiJsonSerializer.serialize(settings, response);
```

### Pattern Permission Check

```java
private static final String RESOURCE_NAME_FOR_PERMISSIONS = "KYC_PROFILE";

// Début de chaque méthode
this.securityContext.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
```

---

**Prêt à migrer ? Commencez par KycProfileApi ! 🚀**
