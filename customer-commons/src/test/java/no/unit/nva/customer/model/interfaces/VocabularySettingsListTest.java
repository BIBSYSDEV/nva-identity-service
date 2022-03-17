package no.unit.nva.customer.model.interfaces;

import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.identityservice.json.JsonConfig;
import org.junit.jupiter.api.Test;

class VocabularySettingsListTest {

    @Test
    void serializationReturnsObjectWithContextEqualToCustomersContext() throws IOException {
        VocabularyList list = randomVocabularyList();
        var json = toJsonMap(list);
        assertThat(json.get(LINKED_DATA_CONTEXT).toString(), is(equalTo(LINKED_DATA_CONTEXT_VALUE.toString())));
    }

    @Test
    void serializationReturnsObjectWithIdEqualToTheGetPathForRetrievingTheVocabularyList() throws IOException {
        CustomerDto customer = CustomerDataGenerator.createSampleCustomerDto();
        VocabularyList list = VocabularyList.fromCustomerDto(customer);
        URI expectedListId = URI.create(customer.getId() + "/vocabularies");
        var json = toJsonMap(list);

        assertThat(customer.getId(), is(not(nullValue())));
        assertThat(json.get(LINKED_DATA_ID).toString(), is(equalTo(expectedListId.toString())));
    }

    private VocabularyList randomVocabularyList() {
        return VocabularyList.fromCustomerDto(CustomerDataGenerator.createSampleCustomerDto());
    }

    private Map<String, Object> toJsonMap(VocabularyList list) throws IOException {
        String jsonString = JsonConfig.asString(list);
        return  JsonConfig.mapFrom(jsonString);
    }
}