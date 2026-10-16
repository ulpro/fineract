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
package org.apache.fineract.crm.interaction.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.crm.interaction.data.CrmTaskData;
import org.apache.fineract.crm.interaction.domain.CrmTask;
import org.apache.fineract.crm.interaction.domain.CrmTaskRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Path("/v1/crm/tasks")
@Component
@Tag(name = "CRM Tâches & Agenda", description = "Gestion des tâches agents et rappels automatiques")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CrmTaskApiResource {

    private final CrmTaskRepository taskRepository;

    @GET
    @Operation(summary = "Lister les tâches", description = "Retourne la liste des tâches filtrées par agent ou par client.")
    public Response retrieveTasks(@QueryParam("userId") final Long userId, @QueryParam("clientId") final Long clientId) {
        List<CrmTask> list;
        if (userId != null) {
            list = taskRepository.findByAssignedToUserId(userId);
        } else if (clientId != null) {
            list = taskRepository.findByClientId(clientId);
        } else {
            list = taskRepository.findAll();
        }

        List<CrmTaskData> dataList = list.stream()
                .map(t -> new CrmTaskData(t.getId(), t.getClientId(), t.getTypeEnum(), t.getSubject(),
                        t.getDescription(), t.getDueDate(), t.getPriorityEnum(), t.getStatusEnum(), t.getAssignedToUserId()))
                .collect(Collectors.toList());

        return Response.ok(dataList).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Créer une nouvelle tâche", description = "Planifie une tâche manuelle ou automatique avec échéance.")
    public Response createTask(final CrmTaskData data) {
        CrmTask task = new CrmTask(
                data.getClientId(),
                data.getTypeEnum() != null ? data.getTypeEnum() : 4, // 4 = MANUAL
                data.getSubject(),
                data.getDescription(),
                data.getDueDate(),
                data.getPriorityEnum(),
                data.getStatusEnum(),
                data.getAssignedToUserId()
        );

        CrmTask saved = taskRepository.save(task);
        CrmTaskData result = new CrmTaskData(saved.getId(), saved.getClientId(), saved.getTypeEnum(), saved.getSubject(),
                saved.getDescription(), saved.getDueDate(), saved.getPriorityEnum(), saved.getStatusEnum(), saved.getAssignedToUserId());

        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @Path("/{taskId}/complete")
    @Transactional
    @Operation(summary = "Marquer une tâche comme terminée", description = "Met à jour le statut de la tâche à COMPLETED.")
    public Response completeTask(@PathParam("taskId") final Long taskId) {
        CrmTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée id: " + taskId));

        task.setStatusEnum(2); // 2 = COMPLETED
        CrmTask saved = taskRepository.save(task);

        CrmTaskData result = new CrmTaskData(saved.getId(), saved.getClientId(), saved.getTypeEnum(), saved.getSubject(),
                saved.getDescription(), saved.getDueDate(), saved.getPriorityEnum(), saved.getStatusEnum(), saved.getAssignedToUserId());

        return Response.ok(result).build();
    }
}
