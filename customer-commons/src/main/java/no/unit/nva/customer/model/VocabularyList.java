package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.customer.model.interfaces.Context;
import no.unit.nva.customer.model.interfaces.Typed;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static no.unit.nva.customer.Constants.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.Constants.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.Constants.LINKED_DATA_ID;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings("PMD.ExcessivePublicCount")
public class VocabularyList implements Context, Typed {

    public static final String VOCABULARY_SETTINGS = "vocabularies";
    public static final String TYPE = "VocabularyList";
    @JsonProperty(VOCABULARY_SETTINGS)
    private Set<VocabularyDto> vocabularies;
    @JsonProperty(LINKED_DATA_ID)
    private URI id;

    public VocabularyList() {

    }

    public VocabularyList(URI id,
                          Collection<VocabularyDto> vocabularies) {
        this.vocabularies = new HashSet<>(vocabularies);
        this.id = id;
    }

    public static VocabularyList fromCustomerDto(CustomerDto customerDto) {

        URI id = UriWrapper.fromUri(customerDto.getId()).addChild(VOCABULARY_SETTINGS).getUri();
        Set<VocabularyDto> vocabularies = new HashSet<>(customerDto.getVocabularies());
        return new VocabularyList(id, vocabularies);
    }

    public static VocabularyList fromJson(String json) {
        return attempt(() -> JsonConfig.readValue(json, VocabularyList.class)).orElseThrow();
    }

    @JacocoGenerated
    @JsonProperty(LINKED_DATA_ID)
    public URI getId() {
        return id;
    }

    @JacocoGenerated
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

    public List<VocabularyDto> getVocabularies() {
        return JacksonJrDoesNotSupportSets.toList(vocabularies);
    }

    public void setVocabularies(List<VocabularyDto> vocabularies) {
        this.vocabularies = JacksonJrDoesNotSupportSets.toSet(vocabularies);
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

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }

    @Override
    @JsonProperty(TYPE_FIELD)
    public String getType() {
        return TYPE;
    }

    @Override
    public void setType(String type) throws BadRequestException {
        Typed.super.setType(type);
    }
}
