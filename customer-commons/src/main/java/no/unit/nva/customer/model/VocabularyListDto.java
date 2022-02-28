package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.customer.model.interfaces.WithContext;
import no.unit.nva.customer.model.interfaces.WithId;
import no.unit.nva.customer.model.interfaces.WithVocabulary;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;

@SuppressWarnings("PMD.ExcessivePublicCount")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = As.PROPERTY,
    property = "type")
@JsonTypeName("VocabularyList")
public class VocabularyListDto implements WithId, WithContext, WithVocabulary<VocabularyDto> {

    public static final String VOCABULARY_SETTINGS = "vocabularies";
    @JsonProperty(VOCABULARY_SETTINGS)
    private Set<VocabularyDto> vocabularies;
    @JsonProperty(LINKED_DATA_ID)
    private URI id;

    @JsonCreator
    public VocabularyListDto(@JsonProperty(LINKED_DATA_ID) URI id,
                             @JsonProperty(VOCABULARY_SETTINGS) Set<VocabularyDto> vocabularies) {
        this.vocabularies = vocabularies;
        this.id = id;
    }

    public static VocabularyListDto fromCustomerDto(CustomerDto customer) {
        URI id = new UriWrapper(customer.getId()).addChild(VOCABULARY_SETTINGS).getUri();
        Set<VocabularyDto> vocabularies = customer.getVocabularies();
        return new VocabularyListDto(id, vocabularies);
    }

    @Override
    public Set<VocabularyDto> getVocabularies() {
        return vocabularies;
    }

    @Override
    @JacocoGenerated
    public void setVocabularies(Set<VocabularyDto> vocabularies) {
        this.vocabularies = vocabularies;
    }

    @JsonProperty(LINKED_DATA_ID)
    @Override
    public URI getId() {
        return id;
    }

    @JacocoGenerated
    @Override
    public void setId(URI id) {
        this.id = id;
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
        if (!(o instanceof VocabularyListDto)) {
            return false;
        }
        VocabularyListDto that = (VocabularyListDto) o;
        return Objects.equals(getVocabularies(), that.getVocabularies());
    }
}
