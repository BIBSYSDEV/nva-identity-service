package no.unit.nva.customer.model;

import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomInstant;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomString;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;

class CustomerDaoTest {

    public static final Javers JAVERS = JaversBuilder.javers().build();

    @Test
    public void toCustomerDtoReturnsDtoWithoutLossOfInformation() {
        CustomerDao expected = CustomerDataGenerator.createSampleCustomerDao();
        CustomerDto customerDto = expected.toCustomerDto();
        CustomerDao actual = CustomerDao.fromCustomerDto(customerDto);
        Diff diff = JAVERS.compare(expected, actual);
        assertThat(customerDto, doesNotHaveEmptyValues());
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void fromCustomerDbReturnsDbWithoutLossOfInformation() {
        CustomerDto expected = crateSampleCustomerDto();
        CustomerDao customerDb = CustomerDao.fromCustomerDto(expected);
        CustomerDto actual = customerDb.toCustomerDto();
        Diff diff = JAVERS.compare(expected, actual);
        assertThat(customerDb, doesNotHaveEmptyValues());
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void daoCanBeDeserializedWhenJsonDoesNotIncludeType() throws JsonProcessingException {
        CustomerDao someDao = sampleCustomerDao();
        ObjectNode json = ObjectMapperConfig.objectMapper.convertValue(someDao, ObjectNode.class);
        json.remove("type");
        CustomerDao deserialized = ObjectMapperConfig.objectMapper.readValue(json.toString(), CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    @Test
    void daoIsSerializedWithType() throws JsonProcessingException {
        CustomerDao someDao = sampleCustomerDao();
        ObjectNode json = ObjectMapperConfig.objectMapper.convertValue(someDao, ObjectNode.class);
        assertThat(json.has("type"), is((true)));

        CustomerDao deserialized = ObjectMapperConfig.objectMapper.readValue(json.toString(), CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    private CustomerDto crateSampleCustomerDto() {
        UUID identifier = UUID.randomUUID();
        URI id = LinkedDataContextUtils.toId(identifier);
        CustomerDto customer = CustomerDto.builder()
            .withName(randomString())
            .withCristinId(randomString())
            .withFeideOrganizationId(randomString())
            .withModifiedDate(randomInstant())
            .withIdentifier(identifier)
            .withId(id)
            .withCname(randomString())
            .withContext(LINKED_DATA_CONTEXT_VALUE)
            .withArchiveName(randomString())
            .withShortName(randomString())
            .withInstitutionDns(randomString())
            .withDisplayName(randomString())
            .withCreatedDate(randomInstant())
            .withVocabularies(randomVocabularyDtoSettings())
            .build();

        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
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

    private CustomerDao sampleCustomerDao() {
        return CustomerDao.builder()
            .withArchiveName("someName")
            .withIdentifier(UUID.randomUUID())
            .build();
    }
}