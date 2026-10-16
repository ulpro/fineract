package org.apache.fineract.accounting.cemac.service;

import org.apache.fineract.accounting.cemac.domain.RefCobacPcemf;
import org.apache.fineract.accounting.cemac.domain.RefCobacPcemfRepository;
import org.apache.fineract.accounting.cemac.serialization.CemacCodeCommandFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CemacCodeWritePlatformServiceImpl implements CemacCodeWritePlatformService {

    private final PlatformSecurityContext context;
    private final RefCobacPcemfRepository repository;
    private final CemacCodeCommandFromApiJsonDeserializer fromApiJsonDeserializer;

    @Autowired
    public CemacCodeWritePlatformServiceImpl(final PlatformSecurityContext context, final RefCobacPcemfRepository repository,
            final CemacCodeCommandFromApiJsonDeserializer fromApiJsonDeserializer) {
        this.context = context;
        this.repository = repository;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
    }

    @Transactional
    @Override
    public CommandProcessingResult createCemacCode(final JsonCommand command) {
        try {
            this.context.authenticatedUser();
            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final String cobacCode = command.stringValueOfParameterNamed("cobacCode");
            final String pcemfCode = command.stringValueOfParameterNamed("pcemfCode");
            final String libelleCobac = command.stringValueOfParameterNamed("libelleCobac");
            final Integer classePcemf = command.integerValueOfParameterNamed("classePcemf");
            final String etatSesame = command.stringValueOfParameterNamed("etatSesame");

            final RefCobacPcemf cemacCode = RefCobacPcemf.create(cobacCode, pcemfCode, libelleCobac, classePcemf, etatSesame);
            
            this.repository.saveAndFlush(cemacCode);

            return new CommandProcessingResultBuilder()
                    .withCommandId(command.commandId())
                    .withEntityId(cemacCode.getId())
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteCemacCode(final Long cemacCodeId) {
        this.context.authenticatedUser();

        final RefCobacPcemf cemacCode = this.repository.findById(cemacCodeId)
                .orElseThrow(() -> new PlatformDataIntegrityException("error.msg.cemac.code.not.found", "Cemac code not found", cemacCodeId));

        this.repository.delete(cemacCode);

        return new CommandProcessingResultBuilder()
                .withEntityId(cemacCodeId)
                .build();
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("uk_cobac_code") || realCause.getMessage().contains("cobac_code")) {
            final String cobacCode = command.stringValueOfParameterNamed("cobacCode");
            throw new PlatformDataIntegrityException("error.msg.cemac.code.duplicate",
                    "Cemac code with value '" + cobacCode + "' already exists", "cobacCode", cobacCode);
        }
        throw new PlatformDataIntegrityException("error.msg.cemac.code.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
