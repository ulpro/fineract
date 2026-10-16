/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.kyc.repository;

import java.util.Optional;
import org.apache.fineract.kyc.domain.KycFeatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KycFeatureStatusRepository extends JpaRepository<KycFeatureStatus, Long>, JpaSpecificationExecutor<KycFeatureStatus> {

    Optional<KycFeatureStatus> findFirstByKycVerification_ClientIdOrderByKycVerification_IdDesc(Long clientId);

    Optional<KycFeatureStatus> findFirstByKycVerification_ClientIdAndKycVerification_KycStatusOrderByKycVerification_IdDesc(Long clientId,
            String kycStatus);
}
