package no.unit.nva.customer.model;

import static no.unit.nva.customer.model.VocabularyListTest.randomVocabulary;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomAllowFileUploadForTypes;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelClaimDtos;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomDoiAgent;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomPublicationWorkflow;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomRightsRetentionStrategy;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomSector;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class CustomerDtoTest {

    @Test
    void shouldUpdateCustomerWithCustomersDoiSecret() {
        var doiAgent = randomActiveCustomer().getDoiAgent().addPassword(randomString());

        var doiSecret = new SecretManagerDoiAgentDao(randomActiveCustomer().getDoiAgent());

        doiSecret.merge(doiAgent);

        assertEquals(doiSecret.getPassword(), doiAgent.getPassword());
    }

    @Test
    void shouldNotSerializeEmptyCristinIdAndRorToEmptyString() throws JsonProcessingException {
        var json = """
                        {
              "cristinId": "",
              "rorId": ""
            }
            """;
        var customer = JsonUtils.dtoObjectMapper.readValue(json, CustomerDto.class);

        assertNull(customer.getCristinId());
        assertNull(customer.getRorId());
    }

    @Test
    void dtoSerializesToJsonAndBack() throws BadRequestException {
        CustomerDto customer = randomInactiveCustomer();
        customer.getDoiAgent().addPassword("****");
        assertThat(customer, doesNotHaveEmptyValues());
        var json = customer.toString();
        var deserialized = CustomerDto.fromJson(json);
        assertThat(deserialized, is(equalTo(customer)));
        assertThat(deserialized, doesNotHaveEmptyValues());
        assertEquals(deserialized.hashCode(), customer.hashCode());
        assertNotEquals(null, deserialized);

        json = customer.getDoiAgent().toString();
        var deserializedDoiAgent = DoiAgentDto.fromJson(json);

        assertEquals(deserializedDoiAgent.hashCode(), customer.getDoiAgent().hashCode());
        assertNotEquals(null, deserializedDoiAgent);
    }

    @Test
    void shouldThrowBadRequestWhenFailingToDeserializeDoiAgent() {
        String invalidJson = randomString();
        Executable action = () -> DoiAgentDto.fromJson(invalidJson);
        var exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(invalidJson));
    }

    @Test
    void dtoSerializesToJsonAndBackWithSecret() throws BadRequestException {
        CustomerDto customer = randomInactiveCustomer();
        customer.getDoiAgent().addPassword("******");
        var json = customer.toString();
        var deserialized = CustomerDto.fromJson(json);

        var deserializedDoiAgent = deserialized.getDoiAgent();
        assertThat(deserializedDoiAgent.toString(), doesNotHaveEmptyValues());
        assertEquals(deserializedDoiAgent, customer.getDoiAgent());
        assertEquals(deserializedDoiAgent.hashCode(), customer.getDoiAgent().hashCode());
        assertNotEquals(null, deserializedDoiAgent);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenFailingToDeserialize() {
        String invalidJson = randomString();
        Executable action = () -> CustomerDto.fromJson(invalidJson);
        var exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(invalidJson));
    }

    @Test
    void shouldReturnIsActiveWhenInactiveFromIsSetInTheFuture() {
        var randomInactiveCustomer = randomInactiveCustomer();
        randomInactiveCustomer.setInactiveFrom(OffsetDateTime.now().plusDays(3).toInstant());
        assertThat(randomInactiveCustomer.isActive(), is(true));
    }

    @Test
    void shouldReturnIsInactiveWhenInactiveIsSetInThePast() {
        var randomInactiveCustomer = randomInactiveCustomer();
        assertThat(randomInactiveCustomer.isActive(), is(false));
    }

    @Test
    void shouldReturnIsActiveWhenInactiveFromIsNotSet() {
        var randomActiveCustomer = randomActiveCustomer();
        assertThat(randomActiveCustomer.isActive(), is(true));
    }

    @Test
    void shouldNotAddChannelClaimWhenChannelIsAlreadyClaimed() {
        var customer = randomActiveCustomer();
        var numberOfClaimsBefore = customer.getChannelClaims().size();

        var alreadyClaimedChannel = customer.getChannelClaims().stream().findFirst().orElseThrow();
        customer.addChannelClaim(alreadyClaimedChannel);
        var numberOfClaimsAfter = customer.getChannelClaims().size();

        assertThat(numberOfClaimsAfter, is(equalTo(numberOfClaimsBefore)));
    }

    private CustomerDto randomActiveCustomer() {
        var customer = randomInactiveCustomer();
        customer.setInactiveFrom(null);
        return customer;
    }

    private CustomerDto randomInactiveCustomer() {
        return CustomerDto.builder()
                   .withCname(randomString())
                   .withIdentifier(UUID.randomUUID())
                   .withDisplayName(randomString())
                   .withInstitutionDns(randomString())
                   .withShortName(randomString())
                   .withArchiveName(randomString())
                   .withName(randomString())
                   .withFeideOrganizationDomain(randomString())
                   .withCristinId(randomUri())
                   .withCustomerOf(randomApplicationDomain())
                   .withCreatedDate(randomInstant())
                   .withModifiedDate(randomInstant())
                   .withVocabularies(randomVocabularies())
                   .withRorId(randomUri())
                   .withPublicationWorkflow(randomPublicationWorkflow())
                   .withDoiAgent(randomDoiAgent(randomString()))
                   .withSector(randomSector())
                   .withNviInstitution(randomBoolean())
                   .withRboInstitution(randomBoolean())
                   .withInactiveFrom(OffsetDateTime.now().minusDays(randomInteger(10)).toInstant())
                   .withAllowFileUploadForTypes(randomAllowFileUploadForTypes())
                   .withRightsRetentionStrategy(randomRightsRetentionStrategy())
                   .withChannelClaims(randomChannelClaimDtos())
                   .build();
    }

    private Collection<VocabularyDto> randomVocabularies() {
        return List.of(randomVocabulary(), randomVocabulary(), randomVocabulary());
    }

    private ApplicationDomain randomApplicationDomain() {
        return randomElement(List.of(ApplicationDomain.values()));
    }
}