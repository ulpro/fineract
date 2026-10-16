package org.apache.fineract.accounting.cemac.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

public interface CemacCodeWritePlatformService {
    CommandProcessingResult createCemacCode(JsonCommand command);
    CommandProcessingResult deleteCemacCode(Long cemacCodeId);
}
