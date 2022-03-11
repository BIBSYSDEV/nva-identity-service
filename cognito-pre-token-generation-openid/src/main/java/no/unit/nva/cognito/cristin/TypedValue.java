package no.unit.nva.cognito.cristin;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class TypedValue {

    private String type;
    private String value;

    @JacocoGenerated
    public String getType() {
        return type;
    }

    @JacocoGenerated
    public void setType(String type) {
        this.type = type;
    }

    @JacocoGenerated
    public String getValue() {
        return value;
    }

    @JacocoGenerated
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.objectMapper.asString(this)).orElseThrow();
    }
}
