package no.unit.nva.customer.model.interfaces;

import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.customer.RestConfig;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyDto;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.ExcessivePublicCount")
public class VocabularyList implements Context, Typed {

    public static final String VOCABULARY_SETTINGS = "vocabularies";
    public static final String TYPE = "VocabularyList";
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

    public static VocabularyList fromJson(String json){
        return attempt(()-> RestConfig.defaultRestObjectMapper.beanFrom(VocabularyList.class,json))
                   .orElseThrow(fail-> new BadRequestException("Could not read input: "+json));
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

    @Override
    @JsonProperty(TYPE_FIELD)
    public String getType() {
        return VocabularyList.TYPE;
    }

    @Override
    public void setType(String type) {
        Typed.super.setType(type);
    }
}
