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

public enum KycAssignmentStatus {

    INVALID(0, "kycAssignmentStatus.invalid"), UNASSIGNED(100, "kycAssignmentStatus.unassigned"), ASSIGNED(200,
            "kycAssignmentStatus.assigned"), IN_REVIEW(300,
                    "kycAssignmentStatus.in.review"), ESCALATED(400, "kycAssignmentStatus.escalated");

    private final Integer value;
    private final String code;

    KycAssignmentStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static KycAssignmentStatus fromInt(final Integer statusValue) {
        if (statusValue == null) {
            return KycAssignmentStatus.INVALID;
        }
        return switch (statusValue) {
            case 100 -> KycAssignmentStatus.UNASSIGNED;
            case 200 -> KycAssignmentStatus.ASSIGNED;
            case 300 -> KycAssignmentStatus.IN_REVIEW;
            case 400 -> KycAssignmentStatus.ESCALATED;
            default -> KycAssignmentStatus.INVALID;
        };
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }
}
