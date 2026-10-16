package org.apache.fineract.accounting.cemac.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "ref_cobac_pcemf", uniqueConstraints = { @UniqueConstraint(columnNames = { "cobac_code" }, name = "uk_cobac_code") })
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class RefCobacPcemf extends AbstractPersistableCustom<Long> {

    @Column(name = "cobac_code", nullable = false, length = 10)
    private String cobacCode;

    @Column(name = "pcemf_code", nullable = false, length = 20)
    private String pcemfCode;

    @Column(name = "libelle_cobac", nullable = true, length = 300)
    private String libelleCobac;

    @Column(name = "classe_pcemf", nullable = true)
    private Integer classePcemf;

    @Column(name = "etat_sesame", nullable = true, length = 20)
    private String etatSesame;

    public static RefCobacPcemf create(final String cobacCode, final String pcemfCode, final String libelleCobac, final Integer classePcemf, final String etatSesame) {
        return new RefCobacPcemf()
                .setCobacCode(cobacCode)
                .setPcemfCode(pcemfCode)
                .setLibelleCobac(libelleCobac)
                .setClassePcemf(classePcemf)
                .setEtatSesame(etatSesame);
    }
}
