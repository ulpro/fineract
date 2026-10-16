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

public enum KycRiskLevel {

    INVALID(0, "kycRiskLevel.invalid"), LOW(100, "kycRiskLevel.low"), MEDIUM(200, "kycRiskLevel.medium"), HIGH(300,
            "kycRiskLevel.high"), CRITICAL(400, "kycRiskLevel.critical");

    private final Integer value;
    private final String code;

    KycRiskLevel(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static KycRiskLevel fromInt(final Integer levelValue) {
        if (levelValue == null) {
            return KycRiskLevel.INVALID;
        }
        return switch (levelValue) {
            case 100 -> KycRiskLevel.LOW;
            case 200 -> KycRiskLevel.MEDIUM;
            case 300 -> KycRiskLevel.HIGH;
            case 400 -> KycRiskLevel.CRITICAL;
            default -> KycRiskLevel.INVALID;
        };
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }
}
