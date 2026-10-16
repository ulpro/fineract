package org.apache.fineract.accounting.cemac.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RefCobacPcemfRepository extends JpaRepository<RefCobacPcemf, Long>, JpaSpecificationExecutor<RefCobacPcemf> {

}
