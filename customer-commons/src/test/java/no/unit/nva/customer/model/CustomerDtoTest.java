package no.unit.nva.customer.model;

import static no.unit.nva.customer.model.VocabularyListTest.randomVocabulary;
import static no.unit.nva.customer.model.interfaces.DoiAgent.randomDoiAgent;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomPublicationWorkflow;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class CustomerDtoTest {

    @Test
    void dtoSerializesToJsonAndBack() throws BadRequestException {
        CustomerDto customer = randomCustomer();
        assertThat(customer, doesNotHaveEmptyValues());
        var json = customer.toString();
        var deserialized = CustomerDto.fromJson(json);
        assertThat(deserialized, is(equalTo(customer)));
        assertThat(deserialized, doesNotHaveEmptyValues());
        assertEquals(deserialized.hashCode(),customer.hashCode());
        assertNotEquals(null, deserialized);

        var doi = deserialized.getDoiAgent();
        assertThat(doi.toString(),doesNotHaveEmptyValues());
        assertEquals(doi,deserialized.getDoiAgent());
        assertEquals(doi.hashCode(),deserialized.getDoiAgent().hashCode());
        assertNotEquals(null, doi);
    }


    @Test
    void dtoSerializesToJsonAndBackWithSecret() throws BadRequestException {
        CustomerDto customer = randomCustomer();
        customer.getDoiAgent().addLink("secret",randomUri().toString());
        var json = customer.toString();
        var deserialized = CustomerDto.fromJson(json);

        var doi = deserialized.getDoiAgent();
        assertThat(doi.toString(),doesNotHaveEmptyValues());
        assertEquals(doi,deserialized.getDoiAgent());
        assertEquals(doi.hashCode(),deserialized.getDoiAgent().hashCode());
        assertNotEquals(null, doi);
    }
    @Test
    void shouldThrowBadRequestExceptionWhenFailingToDeserialize() {
        String invalidJson = randomString();
        Executable action = () -> CustomerDto.fromJson(invalidJson);
        var exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(invalidJson));
    }

    private CustomerDto randomCustomer() {
        return CustomerDto.builder()
                   .withCname(randomString())
                   .withIdentifier(UUID.randomUUID())
                   .withId(randomUri())
                   .withDisplayName(randomString())
                   .withInstitutionDns(randomString())
                   .withContext(randomUri())
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
                   .build();
    }

    private Collection<VocabularyDto> randomVocabularies() {
        return List.of(randomVocabulary(), randomVocabulary(), randomVocabulary());
    }

    private ApplicationDomain randomApplicationDomain() {
        return randomElement(List.of(ApplicationDomain.values()));
    }
}