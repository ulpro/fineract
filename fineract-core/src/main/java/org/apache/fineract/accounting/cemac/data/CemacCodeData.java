package org.apache.fineract.accounting.cemac.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public final class CemacCodeData {

    private final Long id;
    private final String cobacCode;
    private final String pcemfCode;
    private final String libelleCobac;
    private final Integer classePcemf;
    private final String etatSesame;

    public static CemacCodeData instance(final Long id, final String cobacCode, final String pcemfCode, final String libelleCobac,
            final Integer classePcemf, final String etatSesame) {
        return new CemacCodeData(id, cobacCode, pcemfCode, libelleCobac, classePcemf, etatSesame);
    }
}
