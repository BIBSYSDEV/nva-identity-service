package no.unit.nva.customer.model;

import static no.unit.nva.customer.JsonConfig.defaultDynamoConfigMapper;
import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDao;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomString;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;
import java.util.stream.Collectors;

import no.unit.nva.customer.testing.CustomerDataGenerator;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;

class CustomerDaoTest {

    public static final Javers JAVERS = JaversBuilder.javers().build();

    @Test
    public void toCustomerDtoReturnsDtoWithoutLossOfInformation() {
        CustomerDao expected = createSampleCustomerDao();
        CustomerDto customerDto = expected.toCustomerDto();
        CustomerDao actual = CustomerDao.fromCustomerDto(customerDto);
        Diff diff = JAVERS.compare(expected, actual);
        assertThat(customerDto, doesNotHaveEmptyValues());
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void fromCustomerDbReturnsDbWithoutLossOfInformation() {
        CustomerDao expected = CustomerDataGenerator.createSampleCustomerDao();
        CustomerDao actual = CustomerDao.fromCustomerDto(expected.toCustomerDto());
        assertThat(expected, doesNotHaveEmptyValues());
        Diff diff = JAVERS.compare(expected, actual);
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void daoCanBeDeserializedWhenJsonDoesNotIncludeType() throws JsonProcessingException {
        CustomerDao someDao = createSampleCustomerDao();
        ObjectNode json = defaultDynamoConfigMapper.convertValue(someDao, ObjectNode.class);
        json.remove("type");
        CustomerDao deserialized = defaultDynamoConfigMapper.readValue(json.toString(), CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    @Test
    void daoIsSerializedWithType() throws JsonProcessingException {
        CustomerDao someDao = createSampleCustomerDao();
        ObjectNode json = defaultDynamoConfigMapper.convertValue(someDao, ObjectNode.class);
        assertThat(json.has("type"), is((true)));

        CustomerDao deserialized = defaultDynamoConfigMapper.readValue(json.toString(), CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    private Set<VocabularyDto> randomVocabularyDtoSettings() {
        return randomVocabularySettings()
            .stream()
            .map(VocabularyDao::toVocabularySettingsDto)
            .collect(Collectors.toSet());
    }

    private Set<VocabularyDao> randomVocabularySettings() {
        VocabularyDao vocabulary = new VocabularyDao(randomString(), CustomerDataGenerator.randomUri(),
                                                     CustomerDataGenerator.randomElement(VocabularyStatus.values()));
        return Set.of(vocabulary);
    }
}