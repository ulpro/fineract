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

import org.springframework.util.StringUtils;

public enum KycDocumentStatus {

    INVALID(0, "kycDocumentStatusType.invalid"), //
    PENDING(100, "kycDocumentStatusType.pending"), //
    UNDER_REVIEW(200, "kycDocumentStatusType.under.review"), //
    VERIFIED(300, "kycDocumentStatusType.verified"), //
    REJECTED(400, "kycDocumentStatusType.rejected"), //
    EXPIRED(500, "kycDocumentStatusType.expired"); //

    private final Integer value;
    private final String code;

    KycDocumentStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static KycDocumentStatus fromInt(final Integer statusValue) {
        if (statusValue == null) {
            return KycDocumentStatus.INVALID;
        }

        return switch (statusValue) {
            case 100 -> KycDocumentStatus.PENDING;
            case 200 -> KycDocumentStatus.UNDER_REVIEW;
            case 300 -> KycDocumentStatus.VERIFIED;
            case 400 -> KycDocumentStatus.REJECTED;
            case 500 -> KycDocumentStatus.EXPIRED;
            default -> KycDocumentStatus.INVALID;
        };
    }

    public static KycDocumentStatus fromString(final String statusString) {
        if (!StringUtils.hasLength(statusString)) {
            return KycDocumentStatus.INVALID;
        }

        if (statusString.equalsIgnoreCase(KycDocumentStatus.PENDING.toString())) {
            return KycDocumentStatus.PENDING;
        } else if (statusString.equalsIgnoreCase(KycDocumentStatus.UNDER_REVIEW.toString())) {
            return KycDocumentStatus.UNDER_REVIEW;
        } else if (statusString.equalsIgnoreCase(KycDocumentStatus.VERIFIED.toString())) {
            return KycDocumentStatus.VERIFIED;
        } else if (statusString.equalsIgnoreCase(KycDocumentStatus.REJECTED.toString())) {
            return KycDocumentStatus.REJECTED;
        } else if (statusString.equalsIgnoreCase(KycDocumentStatus.EXPIRED.toString())) {
            return KycDocumentStatus.EXPIRED;
        }

        return KycDocumentStatus.INVALID;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }
}
