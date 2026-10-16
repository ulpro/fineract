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
package org.apache.fineract.crm.kyc.jobs;

import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.jobs.service.StepName;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * SF-01-05 : Configuration Spring Batch pour le job de mise à jour
 * de l'expiration des pièces KYC.
 *
 * <p>Ce job est enregistré dans la table {@code job} de Fineract via la
 * migration Liquibase {@code 0256_crm_kyc_expiration_job.xml}.
 * Il s'exécute quotidiennement à 01h00 (cron: {@code 0 0 1 * * ?}).
 */
@Configuration
public class UpdateKycDocumentExpirationConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private UpdateKycDocumentExpirationTasklet tasklet;

    @Bean
    protected Step updateKycDocumentExpirationStep() {
        return new StepBuilder(StepName.UPDATE_KYC_DOCUMENT_EXPIRATION_STEP.name(), jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job updateKycDocumentExpirationJob() {
        return new JobBuilder(JobName.UPDATE_KYC_DOCUMENT_EXPIRATION.name(), jobRepository)
                .start(updateKycDocumentExpirationStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
