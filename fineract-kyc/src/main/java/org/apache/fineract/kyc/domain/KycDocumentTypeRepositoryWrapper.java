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
package org.apache.fineract.kyc.domain;

import org.apache.fineract.kyc.exception.KycDocumentTypeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KycDocumentTypeRepositoryWrapper {

    private final KycDocumentTypeRepository repository;

    @Autowired
    public KycDocumentTypeRepositoryWrapper(final KycDocumentTypeRepository repository) {
        this.repository = repository;
    }

    public KycDocumentType findOneWithNotFoundDetection(final Long id) {
        return this.repository.findById(id).orElseThrow(() -> new KycDocumentTypeNotFoundException(id));
    }
}
