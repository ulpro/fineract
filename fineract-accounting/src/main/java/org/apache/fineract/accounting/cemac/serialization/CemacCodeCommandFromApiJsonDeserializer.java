package org.apache.fineract.accounting.cemac.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CemacCodeCommandFromApiJsonDeserializer {

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public CemacCodeCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, java.util.Set.of("cobacCode", "pcemfCode", "libelleCobac", "classePcemf", "etatSesame", "locale", "dateFormat"));

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("cemaccode");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String cobacCode = this.fromApiJsonHelper.extractStringNamed("cobacCode", element);
        baseDataValidator.reset().parameter("cobacCode").value(cobacCode).notBlank().notExceedingLengthOf(10);

        final String pcemfCode = this.fromApiJsonHelper.extractStringNamed("pcemfCode", element);
        baseDataValidator.reset().parameter("pcemfCode").value(pcemfCode).notBlank().notExceedingLengthOf(20);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
