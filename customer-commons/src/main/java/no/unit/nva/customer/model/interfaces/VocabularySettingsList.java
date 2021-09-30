package no.unit.nva.customer.model.interfaces;

import static no.unit.nva.customer.model.LinkedDataContextUtils.ID_NAMESPACE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Set;
import no.unit.nva.customer.model.LinkedDataContextUtils;
import no.unit.nva.customer.model.VocabularySettingDto;
import nva.commons.core.JacocoGenerated;

public class VocabularySettingsList implements Context {

    public static final String VOCABULARY_SETTINGS = "vocabularySettings";
    @JsonProperty(VOCABULARY_SETTINGS)
    private final Set<VocabularySettingDto> vocabularySettings;

    @JsonCreator
    public <E> VocabularySettingsList(@JsonProperty(VOCABULARY_SETTINGS) Set<VocabularySettingDto> vocabularySettings) {
        this.vocabularySettings = vocabularySettings;
    }

    public Set<VocabularySettingDto> getVocabularySettings() {
        return vocabularySettings;
    }

    @JsonProperty(LinkedDataContextUtils.LINKED_DATA_ID)
    public URI getId() {
        return ID_NAMESPACE;
    }

    @Override
    @JsonProperty(LINKED_DATA_CONTEXT)
    public URI getContext() {
        return LINKED_DATA_CONTEXT_VALUE;
    }

    @JacocoGenerated
    @Override
    public void setContext(URI context) {
        //do nothing
    }
}
