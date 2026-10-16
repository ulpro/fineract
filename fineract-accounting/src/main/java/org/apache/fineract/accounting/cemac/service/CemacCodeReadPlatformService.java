package org.apache.fineract.accounting.cemac.service;

import java.util.Collection;
import org.apache.fineract.accounting.cemac.data.CemacCodeData;

public interface CemacCodeReadPlatformService {
    Collection<CemacCodeData> retrieveAllCemacCodes();
}
