package no.unit.nva.customer.model;

import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomPublicationWorkflow;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.identityservice.json.JsonConfig;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;

class CustomerDaoTest {

    public static final Javers JAVERS = JaversBuilder.javers().build();

    @Test
    void toCustomerDtoReturnsDtoWithoutLossOfInformation() {
        CustomerDao expected = CustomerDataGenerator.createSampleCustomerDao();
        CustomerDto customerDto = expected.toCustomerDto();
        CustomerDao actual = CustomerDao.fromCustomerDto(customerDto);
        Diff diff = JAVERS.compare(expected, actual);
        assertThat(customerDto, doesNotHaveEmptyValues());
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void fromCustomerDbReturnsDbWithoutLossOfInformation() {
        CustomerDto expected = createSampleCustomerDto();
        CustomerDao customerDb = CustomerDao.fromCustomerDto(expected);
        CustomerDto actual = customerDb.toCustomerDto();
        Diff diff = JAVERS.compare(expected, actual);
        assertThat(customerDb, doesNotHaveEmptyValues());
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void daoCanBeDeserializedWhenJsonDoesNotIncludeType() throws IOException {
        CustomerDao someDao = sampleCustomerDao();
        Map<String, Object> jsonMap = customerToJsonMap(someDao);
        jsonMap.remove("type");
        var jsonStringWithoutType = JsonConfig.writeValueAsString(jsonMap);
        CustomerDao deserialized = JsonConfig.readValue(jsonStringWithoutType, CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    @Test
    void daoIsSerializedWithType() throws IOException {
        CustomerDao someDao = sampleCustomerDao();
        var jsonMap = customerToJsonMap(someDao);
        assertThat(jsonMap, hasKey("type"));
        var jsonString = JsonConfig.writeValueAsString(jsonMap);
        CustomerDao deserialized = JsonConfig.readValue(jsonString, CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    private Map<String, Object> customerToJsonMap(CustomerDao someDao) throws IOException {
        var jsonString = JsonConfig.writeValueAsString(someDao);
        return JsonConfig.mapFrom(jsonString);
    }

    private CustomerDto createSampleCustomerDto() {
        UUID identifier = UUID.randomUUID();
        URI id = LinkedDataContextUtils.toId(identifier);
        CustomerDto customer = CustomerDto.builder()
                                   .withName(randomString())
                                   .withCristinId(randomUri())
                                   .withFeideOrganizationDomain(randomString())
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
                                   .withRorId(randomUri())
                                   .withPublicationWorkflow(randomPublicationWorkflow())
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
        VocabularyDao vocabulary = new VocabularyDao(randomString(), randomUri(),
                                                     randomElement(VocabularyStatus.values()));
        return Set.of(vocabulary);
    }

    private CustomerDao sampleCustomerDao() {
        return CustomerDao.builder()
                   .withArchiveName("someName")
                   .withIdentifier(UUID.randomUUID())
                   .build();
    }
}