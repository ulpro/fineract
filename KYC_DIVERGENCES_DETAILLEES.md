# DIVERGENCES DÉTAILLÉES - Module KYC vs Standards Apache Fineract

## 📍 DIVERGENCE 1: Annotations de Classe Racine

### ❌ KycProfileApi, KycDocumentApi, KycWorkflowApi

```java
@RestController                          // ❌ PROBLÉMATIQUE
@RequestMapping("/api/kyc/profiles")     // ❌ PROBLÉMATIQUE
@RequiredArgsConstructor
public class KycProfileApi { }
```

**Problème**:
- `@RestController` est une annotation **Spring MVC** uniquement
- Jersey (système d'exposition d'Apache Fineract) cherche explicitement `@Path`
- JerseyConfig scanne `getBeansWithAnnotation(Path.class)` ← KycProfileApi n'est PAS détecté

**Code JerseyConfig.java preuves**:
```java
@PostConstruct
public void setup() {
    appCtx.getBeansWithAnnotation(Path.class).values().forEach(this::register);
    // ↑ Scanne UNIQUEMENT les beans avec @Path
    // ↑ KycProfileApi (@RestController) N'EST PAS scannée
}
```

### ✅ KycAuditLogApi (Conforme)

```java
@Component                               // ✅ CORRECT
@Path("/v1/kyc/audit")                   // ✅ CORRECT
@Consumes(MediaType.APPLICATION_JSON)    // ✅ CORRECT
@Produces(MediaType.APPLICATION_JSON)    // ✅ CORRECT
@RequiredArgsConstructor
public class KycAuditLogApi { }
```

**Pourquoi ça marche**:
- `@Component` = Spring gère le bean
- `@Path` = Jersey le détecte et l'enregistre dans ResourceConfig

---

## 📍 DIVERGENCE 2: Routings

### ❌ KYC (Non-Standard)

```java
@RequestMapping("/api/kyc/profiles")     // ❌ Pas de /v1
@RequestMapping("/api/kyc")              // ❌ Pas de /v1
```

### ✅ Standard Fineract

```java
@Path("/v1/loans/{loanId}/schedule")              // ✅ /v1/
@Path("/v1/delinquency")                          // ✅ /v1/
@Path("/v1/notifications")                        // ✅ /v1/
@Path("/v1/adhocquery")                           // ✅ /v1/
```

**Implication**:
- APIs Spring = `/api/kyc/*` (ne passent pas par Jersey)
- APIs JAX-RS = `/api/v1/kyc/audit` (passent par Jersey ← `/api` prefix du JerseyConfig)
- **Résultat**: Deux namespace d'URL incompatibles

---

## 📍 DIVERGENCE 3: Verbes HTTP et Binding

### ❌ Spring (KycProfileApi)

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<KycProfileResponse>> getProfileById(
        @PathVariable UUID id) {  // ❌ Spring binding
    ...
}

@PostMapping
public ResponseEntity<ApiResponse<KycProfileResponse>> createProfile(
        @Valid @RequestBody CreateKycProfileRequest request) {  // ❌ Spring binding
    ...
}
```

**Jersey n'interprète PAS**:
- `@GetMapping` → Doit être `@GET`
- `@PathVariable` → Doit être `@PathParam`
- `@RequestBody` → Pas d'équivalent direct (manuelle ou ContentHandler)

### ✅ JAX-RS (KycAuditLogApi)

```java
@GET
public Object getAllAuditLogs(
        @DefaultValue("0") @QueryParam("page") int page,     // ✅ JAX-RS binding
        @DefaultValue("30") @QueryParam("limit") int limit) { // ✅ JAX-RS binding
    ...
}

@GET
@Path("/entity/{entityType}/{entityId}")
public Object getEntityAuditLogs(
        @PathParam("entityType") String entityType,           // ✅ JAX-RS binding
        @PathParam("entityId") UUID entityId,                 // ✅ JAX-RS binding
        @DefaultValue("0") @QueryParam("page") int page,
        @DefaultValue("30") @QueryParam("limit") int limit) {
    ...
}
```

**Jersey interprète correctement**:
- `@GET` → HTTP GET routing
- `@PathParam` → Injection de {entityType}, {entityId}
- `@QueryParam` → Injection de page, limit
- `@DefaultValue` → Valeur par défaut

---

## 📍 DIVERGENCE 4: Content Negotiation

### ❌ Spring (Implicite)

```java
@PostMapping
public ResponseEntity<ApiResponse<KycProfileResponse>> createProfile(
        @Valid @RequestBody CreateKycProfileRequest request) {
    // Spring déduit automatiquement:
    // - Accept: application/json (via paramètre @RequestBody)
    // - Content-Type: application/json (via ResponseEntity)
    // Pas de déclaration explicite dans la méthode
}
```

**Problème**:
- Annotations manquantes: `@Consumes`, `@Produces`
- Jersey nécessite ces déclarations explicites au niveau méthode OU classe

### ✅ JAX-RS (Explicite)

```java
@Component
@Path("/v1/kyc/audit")
@Consumes(MediaType.APPLICATION_JSON)    // ✅ Au niveau classe
@Produces(MediaType.APPLICATION_JSON)    // ✅ Au niveau classe
public class KycAuditLogApi {
    @GET
    public Object getAllAuditLogs(...) {
        // @Consumes, @Produces hérités de la classe
    }
}
```

**Alternative au niveau méthode**:
```java
@POST
@Consumes(MediaType.APPLICATION_JSON)    // ✅ Au niveau méthode
@Produces(MediaType.APPLICATION_JSON)    // ✅ Au niveau méthode
public CommandProcessingResult createDelinquencyRange(
        final DelinquencyRangeRequest request) { ... }
```

---

## 📍 DIVERGENCE 5: Paramètres de Requête

### ❌ Spring (KycProfileApi)

```java
@GetMapping
public ResponseEntity<ApiResponse<PagedResponse<KycProfileResponse>>> getAllProfiles(
        @RequestParam(required = false) KycStatus status,              // ❌ Spring
        @RequestParam(required = false) RiskLevel riskLevel,           // ❌ Spring
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {  // ❌ Spring Data
    ...
}
```

**Problèmes**:
- `@RequestParam` → Jersey attend `@QueryParam`
- `@PageableDefault` → Jersey ne reconnaît pas Spring Data Pageable
- `Pageable` → Type propriétaire Spring (pas standard REST)

### ✅ JAX-RS (Standards Fineract)

```java
@GET
@Path("ranges")
public List<DelinquencyRangeResponse> getDelinquencyRanges(
        @QueryParam("offset") @DefaultValue("0") final Integer offset,  // ✅ JAX-RS
        @QueryParam("limit") @DefaultValue("50") final Integer limit,   // ✅ JAX-RS
        @QueryParam("orderBy") final String orderBy,                    // ✅ JAX-RS
        @QueryParam("sortOrder") final String sortOrder) { ... }
```

**Ou avec SearchParameters (Pattern Fineract)**:
```java
@GET
public String getAllNotifications(
        @Context final UriInfo uriInfo,                         // ✅ JAX-RS context
        @QueryParam("orderBy") final String orderBy,           // ✅ JAX-RS
        @QueryParam("limit") final Integer limit,
        @QueryParam("offset") final Integer offset,
        @QueryParam("sortOrder") final String sortOrder,
        @QueryParam("isRead") final boolean isRead) {

    final SearchParameters searchParameters =
        SearchParameters.builder()
            .limit(limit).offset(offset)
            .orderBy(orderBy).sortOrder(sortOrder)
            .build();

    final ApiRequestJsonSerializationSettings settings =
        this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
    // ↑ Pattern standard Fineract
}
```

---

## 📍 DIVERGENCE 6: Sérialisation/Désérialisation

### ❌ Spring (KycProfileApi)

```java
@PostMapping
public ResponseEntity<ApiResponse<KycProfileResponse>> createProfile(
        @Valid @RequestBody CreateKycProfileRequest request) {  // ❌ Spring
    KycProfileResponse response = kycProfileService.createProfile(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(response, "..."));
}
```

**Spring gère automatiquement**:
- Désérialisation JSON → CreateKycProfileRequest (Jackson)
- Sérialisation CreateKycProfileResponse → JSON (Jackson)
- Codes HTTP via ResponseEntity

### ✅ JAX-RS (Standards Fineract)

```java
@POST
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public CommandProcessingResult createDelinquencyRange(
        final DelinquencyRangeRequest delinquencyRangeRequest) {  // ✅ Paramètre non-annoté
    securityContext.authenticatedUser().validateHasCreatePermission(DELINQUENCY_BUCKET);

    final CommandWrapper commandRequest =
        new CommandWrapperBuilder()
            .createDelinquencyRange()
            .withJson(jsonSerializer.serialize(delinquencyRangeRequest))  // ✅ Sérialisation manuelle
            .build();

    return this.commandWritePlatformService.logCommandSource(commandRequest);
}
```

**Ou avec String JSON brut** (Common Fineract):
```java
@POST
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public String calculateLoanSchedule(
        @PathParam("loanId") final Long loanId,
        @Parameter(hidden = true) final String apiRequestBodyAsJson) {  // ✅ JSON string

    final LoanScheduleData loanSchedule =
        this.calculationPlatformService.generateLoanScheduleForVariableInstallmentRequest(
            loanId, apiRequestBodyAsJson);  // ↑ Parsing manuel

    final ApiRequestJsonSerializationSettings settings =
        this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
    return this.toApiJsonSerializer.serialize(settings, loanSchedule, new HashSet<>());
}
```

---

## 📍 DIVERGENCE 7: Types de Retour

### ❌ Spring (KycProfileApi)

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<KycProfileResponse>> getProfileById(...) {
    // ✓ Retourne ResponseEntity (Spring)
    return ResponseEntity.ok(ApiResponse.ok(response, "..."));
}

@PostMapping
public ResponseEntity<ApiResponse<KycProfileResponse>> createProfile(...) {
    // ✓ Retourne ResponseEntity (Spring)
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(response, "..."));
}
```

### ✅ JAX-RS (Standards Fineract)

```java
@GET
@Path("ranges/{delinquencyRangeId}")
public DelinquencyRangeResponse getDelinquencyRange(
        @PathParam("delinquencyRangeId") final Long delinquencyRangeId) {
    // ✓ Retourne objet typé directement
    return delinquencyResponseMapper.mapRange(
        this.readPlatformService.retrieveDelinquencyRange(delinquencyRangeId));
}

@POST
@Path("ranges")
public CommandProcessingResult createDelinquencyRange(
        final DelinquencyRangeRequest delinquencyRangeRequest) {
    // ✓ Retourne CommandProcessingResult (Pattern Fineract)
    return this.commandWritePlatformService.logCommandSource(commandRequest);
}
```

**Ou String JSON brut**:
```java
@GET
@Produces(MediaType.APPLICATION_JSON)
public String getAllNotifications(...) {
    // ✓ Retourne String JSON (sérialisation manuelle)
    return this.toApiJsonSerializer.serialize(settings, notificationData);
}
```

---

## 📍 DIVERGENCE 8: Annotations Swagger

### ⚠️ Spring (KycProfileApi) - Partiellement Compatible

```java
@PostMapping
@Operation(summary = "...", description = "...")                      // ✅ Swagger v3
@ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201", description = "...", content = ...),   // ✅ Swagger v3
    ...
})
public ResponseEntity<ApiResponse<KycProfileResponse>> createProfile(
        @Valid @RequestBody CreateKycProfileRequest request) {        // ✅ @Valid Swagger v3
    ...
}
```

**Compatible Swagger v3 mais**:
- ❌ Spring génère Swagger séparé (springdoc-openapi) si configuré
- ❌ Jersey génère son propre Swagger (springdoc-openapi-jersey)
- ⚠️ Risque de documentation dupliquée ou fragmentée

### ✅ JAX-RS (KycAuditLogApi) - Pleinement Compatible

```java
@GET
@Path("/entity/{entityType}/{entityId}")
@Operation(summary = "Get Entity Audit Logs")           // ✅ Swagger v3
public Object getEntityAuditLogs(
        @PathParam("entityType") String entityType,     // ✅ JAX-RS
        @PathParam("entityId") UUID entityId,           // ✅ JAX-RS
        @DefaultValue("0") @QueryParam("page") int page,
        @DefaultValue("30") @QueryParam("limit") int limit) {
    ...
}
```

### ⚠️ Problème: Manque @ApiResponse dans KycAuditLogApi

```java
// ❌ INCOMPLET - Manque déclaration réponses HTTP
@GET
@Operation(summary = "List Audit Logs")  // Pas de @ApiResponses
public Object getAllAuditLogs(...) {
    return kycAuditLogService.getAllAuditLogs(page, limit);  // Type: Object
}

// ✅ COMPLET - Pattern Fineract
@GET
@Operation(summary = "List Delinquency Ranges")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(
            implementation = DelinquencyApiResourceSwagger.GetDelinquencyRangesResponse.class)))
})
public List<DelinquencyRangeResponse> getDelinquencyRanges() {
    ...
}
```

---

## 📋 MATRICE SYNTHÉTIQUE DE CONFORMITÉ

```
┌─────────────────────────┬──────────┬──────────┬──────────┬──────────┐
│ Aspect                  │ Profile  │ Document │ Workflow │ AuditLog │
├─────────────────────────┼──────────┼──────────┼──────────┼──────────┤
│ Classe racine           │    ❌    │    ❌    │    ❌    │    ✅    │
│  @Component + @Path     │    ❌    │    ❌    │    ❌    │    ✅    │
├─────────────────────────┼──────────┼──────────┼──────────┼──────────┤
│ Routing                 │    ❌    │    ❌    │    ❌    │    ✅    │
│  @Path(/v1/...)         │    ❌    │    ❌    │    ❌    │    ✅    │
├─────────────────────────┼──────────┼──────────┼──────────┼──────────┤
│ Content negotiation     │    ❌    │    ❌    │    ❌    │    ✅    │
│  @Consumes/@Produces    │    ❌    │    ❌    │    ❌    │    ✅    │
├─────────────────────────┼──────────┼──────────┼──────────┼──────────┤
│ HTTP verbes             │    ❌    │    ❌    │    ❌    │    ✅    │
│  @GET/@POST/@PUT        │    ❌    │    ❌    │    ❌    │    ✅    │
├─────────────────────────┼──────────┼──────────┼──────────┼──────────┤
│ Paramètres              │    ❌    │    ❌    │    ❌    │    ✅    │
│  @PathParam/@QueryParam │    ❌    │    ❌    │    ❌    │    ✅    │
├─────────────────────────┼──────────┼──────────┼──────────┼──────────┤
│ Jersey Exposure         │    ❌    │    ❌    │    ❌    │    ✅    │
├─────────────────────────┼──────────┼──────────┼──────────┼──────────┤
│ Swagger Documentation   │    ⚠️    │    ⚠️    │    ⚠️    │    ⚠️    │
│  (fragmenté)            │    ⚠️    │    ⚠️    │    ⚠️    │    ⚠️    │
├─────────────────────────┼──────────┼──────────┼──────────┼──────────┤
│ Conformité globale      │   🔴 0%  │   🔴 0%  │   🔴 0%  │   🟠 70% │
└─────────────────────────┴──────────┴──────────┴──────────┴──────────┘

Legend:
✅ = Conforme / Compatible
❌ = Non-conforme / Incompatible
⚠️  = Partiellement compatible (risque)
🔴 = Critique (non-exposé par Jersey)
🟠 = Mineur (compatible mais documentation incomplète)
```

---

## 🔗 CHAÎNE D'EXPOSITION RÉELLE

### Cas 1: KycProfileApi (Spring MVC - ❌ Invisible à Jersey)

```
HTTP Request → /api/kyc/profiles
     ↓
Spring DispatcherServlet (@RequestMapping routing)
     ↓
KycProfileApi.createProfile(@RequestBody request)
     ↓
Response: application/json
     ↓
✓ Fonctionne via Spring
✗ Invisible dans Swagger Jersey
✗ Pas cohérent avec autres modules Fineract
```

### Cas 2: KycAuditLogApi (JAX-RS - ✅ Visible à Jersey)

```
HTTP Request → /api/v1/kyc/audit (note: /api est le @ApplicationPath de Jersey)
     ↓
JerseyConfig détecte @Path("/v1/kyc/audit")
     ↓
Jersey ResourceConfig enregistre KycAuditLogApi
     ↓
Jersey route vers KycAuditLogApi.getAllAuditLogs(@QueryParam page, limit)
     ↓
Response: application/json (@Produces)
     ↓
✓ Fonctionne via Jersey
✓ Visible dans Swagger OpenAPI
✓ Cohérent avec autres modules (mais routing différent: /kyc vs /delinquency)
```

---

## 📌 RÉSUMÉ DES DIVERGENCES

| # | Divergence | Spring APIs | JAX-RS API | Impact |
|---|-----------|-------------|-----------|--------|
| 1 | Classe racine | `@RestController` ❌ | `@Component + @Path` ✅ | Jersey ne détecte pas |
| 2 | Routing | `/api/kyc/...` ❌ | `/v1/kyc/audit` ✅ | Namespace incohérent |
| 3 | HTTP verbes | `@GetMapping` ❌ | `@GET` ✅ | Jersey ignore Spring |
| 4 | Content-Type | Implicite ❌ | `@Consumes/@Produces` ✅ | Content negotiation manuel |
| 5 | Paramètres URL | `@PathVariable`, `@RequestParam` ❌ | `@PathParam`, `@QueryParam` ✅ | Jersey n'injecte pas |
| 6 | Pagination | `Pageable` (Spring) ❌ | `@QueryParam offset/limit` ✅ | Incompatible types |
| 7 | Request body | `@RequestBody` ❌ | String JSON ou `@BeanParam` ✅ | Sérialisation différente |
| 8 | Return type | `ResponseEntity<?>` ❌ | Objet ou `String` (JSON) ✅ | Jersey n'interprète pas |
| 9 | Swagger | Fragmented ⚠️ | Partiel (manque @ApiResponse) ⚠️ | Documentation incohérente |
| 10 | Exposition API | ❌ Non via Jersey | ✅ Via Jersey | Invisible à Jersey |

---

## 🎯 ACTION CRITIQUE

**Tous les endpoints KYC sauf KycAuditLogApi ne sont PAS exposés via le système standard d'Apache Fineract (Jersey).**

Cela signifie:
- ❌ Pas d'enregistrement automatique dans OpenAPI/Swagger (si basé sur Jersey)
- ❌ Pas de cohérence avec autres modules
- ❌ Risque de défaillance si migration future vers "Jersey only"
- ❌ Confusion entre deux moteurs d'exposition (Spring + Jersey)

**Nécessité de migration vers JAX-RS pour TOUS les endpoints KYC.**
