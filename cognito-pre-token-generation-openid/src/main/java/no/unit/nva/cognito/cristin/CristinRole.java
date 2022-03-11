package no.unit.nva.cognito.cristin;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Map;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CristinRole {

    private URI id;
    private Map<String, String> labels;

    @JacocoGenerated
    public String getId() {
        return id.toString();
    }

    @JacocoGenerated
    public void setId(String id) {
        this.id = attempt(() -> URI.create(id)).orElseThrow();
    }

    @JacocoGenerated
    public Map<String, String> getLabels() {
        return labels;
    }

    @JacocoGenerated
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }
    @Override
    public String toString(){
        return attempt(()-> JsonConfig.objectMapper.asString(this)).orElseThrow();
    }
}
