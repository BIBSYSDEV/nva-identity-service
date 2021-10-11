package no.unit.nva.customer.model.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

class VocabularySettingsListTest {

    @Test
    void serializationReturnsObjectWithContextEqualToCustomersContext() throws JsonProcessingException {
        VocabularyList list = randomVocabularyList();
        ObjectNode json = toJson(list);
        assertThat(json.get(LINKED_DATA_CONTEXT).textValue(), is(equalTo(LINKED_DATA_CONTEXT_VALUE.toString())));
    }

    @Test
    void serializationReturnsObjectWithIdEqualToTheGetPathForRetrievingTheVocabularyList()
        throws JsonProcessingException {
        CustomerDto customer = CustomerDataGenerator.createSampleCustomerDto();
        VocabularyList list = VocabularyList.fromCustomerDto(customer);
        URI expectedListId = URI.create(customer.getId() + "/vocabularies");
        ObjectNode json = toJson(list);

        assertThat(customer.getId(), is(not(nullValue())));
        assertThat(json.get(LINKED_DATA_ID).textValue(), is(equalTo(expectedListId.toString())));
    }

    private VocabularyList randomVocabularyList() {
        return VocabularyList.fromCustomerDto(CustomerDataGenerator.createSampleCustomerDto());
    }

    private ObjectNode toJson(VocabularyList list) throws JsonProcessingException {
        String jsonString = defaultRestObjectMapper.writeValueAsString(list);
        return (ObjectNode) defaultRestObjectMapper.readTree(jsonString);
    }
}