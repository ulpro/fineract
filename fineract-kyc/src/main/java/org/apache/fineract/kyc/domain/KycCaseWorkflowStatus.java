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

public enum KycCaseWorkflowStatus {

    INVALID(0, "kycCaseWorkflowStatus.invalid"), EN_ATTENTE(100, "kycCaseWorkflowStatus.en.attente"), CONFORME(200,
            "kycCaseWorkflowStatus.conforme"), EN_REVISION(300, "kycCaseWorkflowStatus.en.revision"), RENFORCE(400,
                    "kycCaseWorkflowStatus.renforce"), REJETE(500,
                            "kycCaseWorkflowStatus.rejete"), FERME(600, "kycCaseWorkflowStatus.ferme");

    private final Integer value;
    private final String code;

    KycCaseWorkflowStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static KycCaseWorkflowStatus fromInt(final Integer statusValue) {
        if (statusValue == null) {
            return KycCaseWorkflowStatus.INVALID;
        }
        return switch (statusValue) {
            case 100 -> KycCaseWorkflowStatus.EN_ATTENTE;
            case 200 -> KycCaseWorkflowStatus.CONFORME;
            case 300 -> KycCaseWorkflowStatus.EN_REVISION;
            case 400 -> KycCaseWorkflowStatus.RENFORCE;
            case 500 -> KycCaseWorkflowStatus.REJETE;
            case 600 -> KycCaseWorkflowStatus.FERME;
            default -> KycCaseWorkflowStatus.INVALID;
        };
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }
}
