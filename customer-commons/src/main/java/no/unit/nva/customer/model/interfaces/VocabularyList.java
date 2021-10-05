package no.unit.nva.customer.model.interfaces;

import static no.unit.nva.customer.model.LinkedDataContextUtils.ID_NAMESPACE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.customer.model.LinkedDataContextUtils;
import no.unit.nva.customer.model.VocabularyDto;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.ExcessivePublicCount")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = As.PROPERTY,
    property = "type")
@JsonTypeName("VocabularyList")
public class VocabularyList implements Context {

    public static final String VOCABULARY_SETTINGS = "vocabularies";
    @JsonProperty(VOCABULARY_SETTINGS)
    private final Set<VocabularyDto> vocabularies;

    @JsonCreator
    public <E> VocabularyList(@JsonProperty(VOCABULARY_SETTINGS) Set<VocabularyDto> vocabularySettings) {
        this.vocabularies = vocabularySettings;
    }

    public Set<VocabularyDto> getVocabularies() {
        return vocabularies;
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

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getVocabularies());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VocabularyList)) {
            return false;
        }
        VocabularyList that = (VocabularyList) o;
        return Objects.equals(getVocabularies(), that.getVocabularies());
    }
}
