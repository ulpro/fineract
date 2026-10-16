package org.apache.fineract.accounting.cemac.handler;

import org.apache.fineract.accounting.cemac.service.CemacCodeWritePlatformService;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommandType(entity = "CEMACCODE", action = "CREATE")
public class CreateCemacCodeCommandHandler implements NewCommandSourceHandler {

    private final CemacCodeWritePlatformService writePlatformService;

    @Autowired
    public CreateCemacCodeCommandHandler(final CemacCodeWritePlatformService writePlatformService) {
        this.writePlatformService = writePlatformService;
    }

    @Transactional
    @Override
    public CommandProcessingResult processCommand(final JsonCommand command) {
        return this.writePlatformService.createCemacCode(command);
    }
}
