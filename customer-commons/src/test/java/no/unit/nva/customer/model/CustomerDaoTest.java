package no.unit.nva.customer.model;

import static no.unit.nva.customer.testing.CustomerDataGenerator.randomDoiAgent;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomAllowFileUploadForTypes;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomPublicationWorkflow;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomRightsRetentionStrategy;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomSector;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
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
        assertThat(customerDto, doesNotHaveEmptyValuesIgnoringFields(Set.of("doiAgent.password")));

        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
        assertNotEquals(null, actual);

        var actualDoiAgent = actual.getDoiAgent();
        assertThat(actualDoiAgent.toString(),doesNotHaveEmptyValues());
        assertEquals(actualDoiAgent,expected.getDoiAgent());
        assertEquals(actualDoiAgent.hashCode(),expected.getDoiAgent().hashCode());
        assertNotEquals(null, actualDoiAgent);

        assertThrows(IllegalStateException.class,() -> actual.setType("NOT A TYPE"));
    }

    @Test
    void testingJacocoCoverageAssignNullWorking() {
        var fullAvTull =
            CustomerDao.builder()
                .withCreatedDate((String) null)
                .withModifiedDate((String) null)
                .build();
        assertNotNull(fullAvTull);
    }

    @Test
    void toCustomerDaoToDtoAndBackReturnsWithLossOfSecret() {
        var expectedDao = CustomerDataGenerator.createSampleCustomerDao();
        var customerDto = expectedDao.toCustomerDto();
        customerDto
            .getDoiAgent()
            .addPassword(randomString());
        var actualDao = CustomerDao.fromCustomerDto(customerDto);
        var actualDoiAgent = actualDao.getDoiAgent();

        assertEquals(actualDoiAgent,expectedDao.getDoiAgent());
    }

    @Test
    void fromCustomerDbReturnsDbWithoutLossOfInformation() {
        CustomerDao expected = createSampleCustomerDao();
        CustomerDto customerDto = expected.toCustomerDto();
        CustomerDao actual = CustomerDao.fromCustomerDto(customerDto);
        Diff diff = JAVERS.compare(expected, actual);
        assertThat(actual, doesNotHaveEmptyValues());
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void daoCanBeDeserializedWhenJsonDoesNotIncludeType() throws IOException {
        CustomerDao someDao = createSampleCustomerDao();
        Map<String, Object> jsonMap = customerToJsonMap(someDao);
        jsonMap.remove("type");
        var jsonStringWithoutType = JsonConfig.writeValueAsString(jsonMap);
        CustomerDao deserialized = JsonConfig.readValue(jsonStringWithoutType, CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    @Test
    void daoIsSerializedWithType() throws IOException {
        CustomerDao someDao = createSampleCustomerDao();
        var jsonMap = customerToJsonMap(someDao);
        assertThat(jsonMap, hasKey("type"));
        var jsonString = JsonConfig.writeValueAsString(jsonMap);
        CustomerDao deserialized = JsonConfig.readValue(jsonString, CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    @Test
    void shouldMigrateOldStyleDao() throws IOException {
        var template = """
                {
                  "identifier" : "4fa3622d-877c-4759-b63f-d7d37cf26b5d",
                  "createdDate" : "1985-06-22T21:25:11.558Z",
                  "modifiedDate" : "2020-05-18T00:49:35.971Z",
                  "name" : "5jA24q1K8xRjnX",
                  "displayName" : "HpP3PceJ3eI",
                  "shortName" : "AzObKaEO77a",
                  "archiveName" : "ToPiKGbSOE0KvFTm2k7",
                  "cname" : "gQcgg9mhSH3e27c9mi",
                  "institutionDns" : "QoeKDoyM3UcRhcUF",
                  "feideOrganizationDomain" : "jie3k8uRwHcVhx",
                  "cristinId" : "https://www.example.com/0q7bcwf4zf1k6",
                  "customerOf" : "nva.unit.no",
                  "vocabularies" : [ {
                    "type" : "Vocabulary",
                    "name" : "cZkpIKoQze3EVv0Xm",
                    "id" : "https://www.example.com/kdK0gJxdysnDOZ2aU",
                    "status" : "Allowed"
                  } ],
                  "rorId" : "https://www.example.com/NS9SygkPsLcQU",
                  "publicationWorkflow" : "RegistratorRequiresApprovalForMetadataAndFiles",
                  "doiAgent" : {
                    "prefix" : "10.000",
                    "url" : "mds.X6wSynOURzWwNn2.datacite.org",
                    "username" : "user-name-X6wSynOURzWwNn2"
                  },
                  "nviInstitution" : true,
                  "rboInstitution" : false,
                  "sector" : "INSTITUTE",
                  "rightRetentionStrategy" : {
                    "retentionStrategy" : "%s",
                    "id" : "%s"
                  },
                  "type" : "Customer"
                }
                """;
        var rightsRetentionStrategy = "RightsRetentionStrategy";
        var uri = "https://www.example.com/ZxZeIEHmgcmlAEG5";
        var json = String.format(template, rightsRetentionStrategy, uri);
        var deserialized = JsonConfig.readValue(json, CustomerDao.class);
        var rrs = deserialized.getRightsRetentionStrategy();
        assertThat(rrs.getId().toString(), is(equalTo(uri)));
        assertThat(rrs.getType().toString(), is(equalTo(rightsRetentionStrategy)));
    }

    private Map<String, Object> customerToJsonMap(CustomerDao someDao) throws IOException {
        var jsonString = JsonConfig.writeValueAsString(someDao);
        return JsonConfig.mapFrom(jsonString);
    }

    private CustomerDao createSampleCustomerDao() {
        UUID identifier = UUID.randomUUID();
        return CustomerDao
                   .builder()
                   .withName(randomString())
                   .withCristinId(randomUri())
                   .withCustomerOf(randomApplicationDomain().getUri())
                   .withFeideOrganizationDomain(randomString())
                   .withModifiedDate(randomInstant())
                   .withIdentifier(identifier)
                   .withCname(randomString())
                   .withArchiveName(randomString())
                   .withShortName(randomString())
                   .withInstitutionDns(randomString())
                   .withDisplayName(randomString())
                   .withCreatedDate(randomInstant())
                   .withRorId(randomUri())
                   .withVocabularySettings(randomVocabularySettings())
                   .withPublicationWorkflow(randomPublicationWorkflow())
                   .withDoiAgent(randomDoiAgent(randomString()))
                   .withNviInstitution(randomBoolean())
                   .withRboInstitution(randomBoolean())
                   .withSector(randomSector())
                   .withAllowFileUploadForTypes(randomAllowFileUploadForTypes())
                   .withRightsRetentionStrategy(randomRightsRetentionStrategy())
                   .build();
    }


    private ApplicationDomain randomApplicationDomain() {
        return ApplicationDomain.NVA;
    }

    private Set<VocabularyDto> randomVocabularyDtoSettings() {
        return randomVocabularySettings().stream()
                   .map(VocabularyDao::toVocabularySettingsDto)
                   .collect(Collectors.toSet());
    }

    private Set<VocabularyDao> randomVocabularySettings() {
        VocabularyDao vocabulary = new VocabularyDao(randomString(), randomUri(),
                                                     randomElement(VocabularyStatus.values()));
        return Set.of(vocabulary);
    }
}
