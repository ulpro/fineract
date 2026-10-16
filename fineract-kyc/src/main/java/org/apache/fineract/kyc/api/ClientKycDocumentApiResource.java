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

import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;

import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.contentstore.detector.ContentDetectorContext;
import org.apache.fineract.infrastructure.contentstore.detector.ContentDetectorManager;
import org.apache.fineract.infrastructure.contentstore.service.ContentStoreService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.documentmanagement.api.FileUploadValidator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.kyc.data.KycDocumentData;
import org.apache.fineract.kyc.service.KycDocumentReadPlatformService;
import org.apache.fineract.util.StreamResponseUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

@Path("/v1/clients/{clientId}/kyc/documents")
@Component
@Tag(name = "Client KYC Documents", description = "Manage KYC documents for a specific client.")
@RequiredArgsConstructor
public class ClientKycDocumentApiResource {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = KycApiConstants.KYC_DOCUMENT_RESOURCE_NAME;

    private final PlatformSecurityContext context;
    private final KycDocumentReadPlatformService readPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<KycDocumentData> toApiJsonSerializer;
    private final ContentStoreService contentStoreService;
    private final FileUploadValidator fileUploadValidator;
    private final ContentDetectorManager contentDetectorManager;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List all KYC documents for a Client", description = "Returns all KYC documents associated with the given client identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = KycDocumentData.class)))) })
    public List<KycDocumentData> retrieveAll(@PathParam("clientId") @Parameter(description = "clientId") final Long clientId) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        return this.readPlatformService.retrieveAllByClientId(clientId);
    }

    @GET
    @Path("{documentId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a Client KYC Document", description = "Returns a single KYC document by its identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = KycDocumentData.class))) })
    public KycDocumentData retrieveOne(@PathParam("clientId") @Parameter(description = "clientId") final Long clientId,
            @PathParam("documentId") @Parameter(description = "documentId") final Long documentId) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        return this.readPlatformService.retrieveOne(documentId);
    }

    @GET
    @Path("{documentId}/attachment")
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    @Operation(summary = "Retrieve Binary File associated with KYC Document", description = "Request used to download the file associated with the KYC document.")
    @ApiResponse(responseCode = "200", description = "Not Shown: The corresponding Binary file")
    public Response downloadFile(@PathParam("clientId") @Parameter(description = "clientId") final Long clientId,
            @PathParam("documentId") @Parameter(description = "documentId") final Long documentId) {

        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        final var content = readPlatformService.retrieveDocumentContent(documentId);

        return StreamResponseUtil.ok(StreamResponseUtil.StreamResponseData.builder().type(content.getContentType())
                .fileName(content.getFileName()).stream(content.getStream()).build());
    }

    @POST
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a KYC Document for a Client", description = "Creates a new KYC document for the specified client. Note: A document is created using a Multi-part form upload.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Upload KYC Document", content = {
            @Content(mediaType = MediaType.MULTIPART_FORM_DATA) })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CommandProcessingResult.class))) })
    public CommandProcessingResult create(@PathParam("clientId") @Parameter(description = "clientId") final Long clientId,
            @HeaderParam(CONTENT_LENGTH) final Long fileSize, @FormDataParam("file") final java.io.InputStream is,
            @FormDataParam("file") final FormDataContentDisposition fileDetails, @FormDataParam("file") final FormDataBodyPart filePart,
            @FormDataParam("documentTypeId") final String documentTypeId, @FormDataParam("documentNumber") final String documentNumber,
            @FormDataParam("issueDate") final String issueDate, @FormDataParam("expiryDate") final String expiryDate,
            @FormDataParam("remarks") final String remarks, @FormDataParam("dateFormat") final String dateFormat,
            @FormDataParam("locale") final String locale) {

        fileUploadValidator.validate(fileSize, is, fileDetails, filePart);

        String fileLocation = null;
        if (is != null && fileDetails != null) {
            String path = String.join(contentStoreService.getDelimiter(), "kycdocuments", String.valueOf(clientId),
                    fileDetails.getFileName());

            String type = java.util.Optional.ofNullable(filePart).map(FormDataBodyPart::getMediaType).map(MediaType::toString)
                    .or(() -> java.util.Optional.of(contentDetectorManager
                            .detect(ContentDetectorContext.builder().fileName(fileDetails.getFileName()).build()).getMimeType()))
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            fileLocation = contentStoreService.upload(path, is, type);
        }

        JsonObject json = new JsonObject();
        if (documentTypeId != null) {
            json.addProperty("documentTypeId", Long.parseLong(documentTypeId));
        }
        if (documentNumber != null) {
            json.addProperty("documentNumber", documentNumber);
        }
        if (issueDate != null) {
            json.addProperty("issueDate", issueDate);
        }
        if (expiryDate != null) {
            json.addProperty("expiryDate", expiryDate);
        }
        if (remarks != null) {
            json.addProperty("remarks", remarks);
        }
        if (dateFormat != null) {
            json.addProperty("dateFormat", dateFormat);
        }
        if (locale != null) {
            json.addProperty("locale", locale);
        }
        if (fileLocation != null) {
            json.addProperty("fileLocation", fileLocation);
        }

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createKycDocument(clientId).withJson(json.toString()).build();
        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @PUT
    @Path("{documentId}")
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update a Client KYC Document", description = "Updates an existing KYC document for the specified client.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CommandProcessingResult.class))) })
    public CommandProcessingResult update(@PathParam("clientId") @Parameter(description = "clientId") final Long clientId,
            @PathParam("documentId") @Parameter(description = "documentId") final Long documentId,
            @HeaderParam(CONTENT_LENGTH) final Long fileSize, @FormDataParam("file") final java.io.InputStream is,
            @FormDataParam("file") final FormDataContentDisposition fileDetails, @FormDataParam("file") final FormDataBodyPart filePart,
            @FormDataParam("documentTypeId") final String documentTypeId, @FormDataParam("documentNumber") final String documentNumber,
            @FormDataParam("issueDate") final String issueDate, @FormDataParam("expiryDate") final String expiryDate,
            @FormDataParam("remarks") final String remarks, @FormDataParam("dateFormat") final String dateFormat,
            @FormDataParam("locale") final String locale, @FormDataParam("verificationStatus") final String verificationStatus) {

        String fileLocation = null;
        if (is != null && fileDetails != null) {
            fileUploadValidator.validate(fileSize, is, fileDetails, filePart);
            String path = String.join(contentStoreService.getDelimiter(), "kycdocuments", String.valueOf(clientId),
                    fileDetails.getFileName());
            String type = java.util.Optional.ofNullable(filePart).map(FormDataBodyPart::getMediaType).map(MediaType::toString)
                    .or(() -> java.util.Optional.of(contentDetectorManager
                            .detect(ContentDetectorContext.builder().fileName(fileDetails.getFileName()).build()).getMimeType()))
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            fileLocation = contentStoreService.upload(path, is, type);
        }

        JsonObject json = new JsonObject();
        if (documentTypeId != null) {
            json.addProperty("documentTypeId", Long.parseLong(documentTypeId));
        }
        if (documentNumber != null) {
            json.addProperty("documentNumber", documentNumber);
        }
        if (issueDate != null) {
            json.addProperty("issueDate", issueDate);
        }
        if (expiryDate != null) {
            json.addProperty("expiryDate", expiryDate);
        }
        if (remarks != null) {
            json.addProperty("remarks", remarks);
        }
        if (dateFormat != null) {
            json.addProperty("dateFormat", dateFormat);
        }
        if (locale != null) {
            json.addProperty("locale", locale);
        }
        if (fileLocation != null) {
            json.addProperty("fileLocation", fileLocation);
        }
        if (verificationStatus != null) {
            json.addProperty("verificationStatus", Integer.parseInt(verificationStatus));
        }

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateKycDocument(clientId, documentId).withJson(json.toString())
                .build();
        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }
}
