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
package org.apache.fineract.selfservice.useradministration.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.organisation.staff.exception.StaffNotFoundException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.selfservice.registration.SelfServiceApiConstants;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserClientMapping;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserClientMappingRepository;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserRepository;
import org.apache.fineract.selfservice.useradministration.domain.SelfServiceUserDomainService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.PasswordValidationPolicy;
import org.apache.fineract.useradministration.domain.PasswordValidationPolicyRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.apache.fineract.useradministration.exception.RoleNotFoundException;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SelfServiceUserWritePlatformServiceImpl implements SelfServiceUserWritePlatformService {

    private final PlatformSecurityContext context;
    private final AppUserRepository appUserRepository;
    private final AppSelfServiceUserRepository appSelfServiceUserRepository;
    private final AppSelfServiceUserClientMappingRepository appUserClientMappingRepository;
    private final ClientRepositoryWrapper clientRepository;
    private final PasswordValidationPolicyRepository passwordValidationPolicy;
    private final PlatformPasswordEncoder platformPasswordEncoder;
    private final RoleRepository roleRepository;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final StaffRepository staffRepository;
    private final SelfServiceUserDomainService userDomainService;

    @Transactional
    @Override
    public CommandProcessingResult createUser(final JsonCommand command) {
        // Critique #2: Security permission check
        final AppUser currentUser = this.context.authenticatedUser();
        currentUser.validateHasPermissionTo("CREATE_SELFSERVICEUSER");

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("selfserviceuser");

        // Validate basic inputs
        final String username = command.stringValueOfParameterNamed("username");
        baseDataValidator.reset().parameter("username").value(username).notBlank().notNull().notExceedingLengthOf(100);

        final String email = command.stringValueOfParameterNamed("email");
        baseDataValidator.reset().parameter("email").value(email).notBlank().notNull().notExceedingLengthOf(100);

        final String firstname = command.stringValueOfParameterNamed("firstname");
        baseDataValidator.reset().parameter("firstname").value(firstname).notBlank().notNull().notExceedingLengthOf(100);

        final String lastname = command.stringValueOfParameterNamed("lastname");
        baseDataValidator.reset().parameter("lastname").value(lastname).notBlank().notNull().notExceedingLengthOf(100);

        final String password = command.stringValueOfParameterNamed("password");
        baseDataValidator.reset().parameter("password").value(password).notBlank().notNull().notExceedingLengthOf(100);

        final Long officeId = command.longValueOfParameterNamed("officeId");
        baseDataValidator.reset().parameter("officeId").value(officeId).notNull();

        final Long clientId = command.longValueOfParameterNamed("clientId");
        baseDataValidator.reset().parameter("clientId").value(clientId).notNull();

        Boolean sendPasswordToEmail = command.booleanObjectValueOfParameterNamed("sendPasswordToEmail");
        if (sendPasswordToEmail == null) {
            sendPasswordToEmail = true;
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

        // Critique #6: Uniqueness check across both m_appuser and m_appselfservice_user tables
        final AppUser existingCoreUser = this.appUserRepository.findAppUserByName(username);
        final AppSelfServiceUser existingSelfServiceUser = this.appSelfServiceUserRepository.findAppSelfServiceUserByName(username);
        if (existingCoreUser != null || existingSelfServiceUser != null) {
            throw new PlatformDataIntegrityException("error.msg.user.duplicate.username",
                    "User with username " + username + " already exists.", "username", username);
        }

        final AppUser existingCoreUserByEmail = this.appUserRepository.findActiveUserByEmail(email);
        final AppSelfServiceUser existingSelfServiceUserByEmail = this.appSelfServiceUserRepository.findAppSelfServiceUserByEmail(email);
        if (existingCoreUserByEmail != null || existingSelfServiceUserByEmail != null) {
            throw new PlatformDataIntegrityException("error.msg.user.duplicate.email",
                    "User with email " + email + " already exists.", "email", email);
        }

        // Critique #3: Server-side validation of client existence and office hierarchy permissions
        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
        final String clientHierarchy = client.getOffice().getHierarchy();
        final String userHierarchy = currentUser.getOffice().getHierarchy();
        if (!clientHierarchy.startsWith(userHierarchy)) {
            throw new ClientNotFoundException(clientId);
        }

        // Critique #5: Password complexity validation on backend
        final PasswordValidationPolicy validationPolicy = this.passwordValidationPolicy.findActivePasswordValidationPolicy();
        baseDataValidator.reset().parameter("password").value(password)
                .matchesRegularExpression(validationPolicy.getRegex(), validationPolicy.getDescription());
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

        // Validate unique mapping for client
        final AppSelfServiceUserClientMapping existingMapping = this.appUserClientMappingRepository.fetchByClientId(clientId);
        if (existingMapping != null) {
            throw new PlatformDataIntegrityException("error.msg.self.service.client.already.mapped",
                    "Client with ID " + clientId + " is already mapped to a self-service user.", "clientId", clientId);
        }

        // Assemble Office, Staff and Roles
        final Office userOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);

        final Long staffId = command.longValueOfParameterNamed("staffId");
        Staff linkedStaff = null;
        if (staffId != null) {
            linkedStaff = this.staffRepository.findByOffice(staffId, userOffice.getId())
                    .orElseThrow(() -> new StaffNotFoundException(staffId));
        }

        final Set<Role> allRoles = new HashSet<>();
        final Role role = this.roleRepository.getRoleByName(SelfServiceApiConstants.SELF_SERVICE_USER_ROLE);
        if (role == null) {
            throw new RoleNotFoundException(SelfServiceApiConstants.SELF_SERVICE_USER_ROLE);
        }
        allRoles.add(role);

        // Create the AppSelfServiceUser
        final Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("DUMMY_ROLE_NOT_USED_OR_PERSISTED_TO_AVOID_EXCEPTION"));
        final User springUser = new User(username, password, authorities);

        final AppSelfServiceUser appUser = new AppSelfServiceUser(userOffice, springUser, allRoles, email,
                firstname, lastname, linkedStaff, true, true, Collections.emptyList(), false);

        // Hashing the password (Critique #1: BCrypt used, password never logged in clear)
        this.userDomainService.create(appUser, sendPasswordToEmail);

        // Save mapping
        this.appUserClientMappingRepository.saveClientUserMapping(appUser.getId(), client.getId());

        // Improvement A: Return rich CommandProcessingResult for audit trail
        return new CommandProcessingResultBuilder()
                .withCommandId(command.commandId())
                .withEntityId(appUser.getId())
                .withOfficeId(userOffice.getId())
                .withClientId(client.getId())
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult updateUserStatus(final Long userId, final JsonCommand command) {
        // Critique #2: Security permission check
        final AppUser currentUser = this.context.authenticatedUser();
        currentUser.validateHasPermissionTo("UPDATE_SELFSERVICEUSER");

        final AppSelfServiceUser userToUpdate = this.appSelfServiceUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (userToUpdate.isDeleted()) {
            throw new UserNotFoundException(userId);
        }

        final Boolean active = command.booleanObjectValueOfParameterNamed("active");
        if (active == null) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                    "Missing 'active' parameter.", List.of(ApiParameterError.parameterError("error.msg.user.active.missing",
                            "Missing 'active' parameter", "active")));
        }

        if (active) {
            userToUpdate.enable();
        } else {
            userToUpdate.disable();
        }

        this.appSelfServiceUserRepository.saveAndFlush(userToUpdate);

        return new CommandProcessingResultBuilder()
                .withCommandId(command.commandId())
                .withEntityId(userId)
                .withOfficeId(userToUpdate.getOffice().getId())
                .build();
    }
}
