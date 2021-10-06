package no.unit.nva.customer.model.interfaces;

import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyDto;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

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
    @JsonProperty(LINKED_DATA_ID)
    private final URI id;

    @JsonCreator
    public VocabularyList(@JsonProperty(LINKED_DATA_ID) URI id,
                          @JsonProperty(VOCABULARY_SETTINGS) Set<VocabularyDto> vocabularies) {
        this.vocabularies = vocabularies;
        this.id = id;
    }

    public static VocabularyList fromCustomerDto(CustomerDto customerDto) {
        URI id = new UriWrapper(customerDto.getId()).addChild(VOCABULARY_SETTINGS).getUri();
        Set<VocabularyDto> vocabularies = customerDto.getVocabularies();
        return new VocabularyList(id, vocabularies);
    }

    public Set<VocabularyDto> getVocabularies() {
        return vocabularies;
    }

    @JsonProperty(LINKED_DATA_ID)
    public URI getId() {
        return id;
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
