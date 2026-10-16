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

public enum KycCaseFlagStatus {

    ACTIVE(100, "kycCaseFlagStatus.active"), RESOLVED(200, "kycCaseFlagStatus.resolved"), FALSE_POSITIVE(300,
            "kycCaseFlagStatus.falsePositive");

    private final Integer value;
    private final String code;

    KycCaseFlagStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static KycCaseFlagStatus fromInt(final Integer statusValue) {
        if (statusValue == null) {
            return null;
        }

        switch (statusValue) {
            case 100:
                return KycCaseFlagStatus.ACTIVE;
            case 200:
                return KycCaseFlagStatus.RESOLVED;
            case 300:
                return KycCaseFlagStatus.FALSE_POSITIVE;
            default:
                return null;
        }
    }
}
