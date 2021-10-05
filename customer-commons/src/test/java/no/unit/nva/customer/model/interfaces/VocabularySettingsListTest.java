package no.unit.nva.customer.model.interfaces;

import static no.unit.nva.customer.model.LinkedDataContextUtils.ID_NAMESPACE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

class VocabularySettingsListTest {

    @Test
    void serializationReturnsObjectWithContextEqualToCustomersContext() throws JsonProcessingException {
        VocabularyList list = new VocabularyList(Collections.emptySet());
        ObjectNode json = toJson(list);
        assertThat(json.get(LINKED_DATA_CONTEXT).textValue(), is(equalTo(LINKED_DATA_CONTEXT_VALUE.toString())));
    }

    @Test
    void serializationReturnsObjectWithIdEqualToTheNamespaceOfCustomers() throws JsonProcessingException {
        VocabularyList list = new VocabularyList(Collections.emptySet());
        ObjectNode json = toJson(list);
        assertThat(json.get(LINKED_DATA_ID).textValue(), is(equalTo(ID_NAMESPACE.toString())));
    }

    private ObjectNode toJson(VocabularyList list) throws JsonProcessingException {
        String jsonString = JsonUtils.objectMapperWithEmpty.writeValueAsString(list);
        return (ObjectNode) JsonUtils.objectMapperWithEmpty.readTree(jsonString);
    }
}