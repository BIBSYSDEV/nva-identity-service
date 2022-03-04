package no.unit.nva.customer.model;

import static no.unit.nva.customer.model.VocabularyListTest.randomVocabulary;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class CustomerDtoTest {

    @Test
    void dtoSerializesToJsonAndBack() {
        CustomerDto customer = randomCustomer();
        assertThat(customer, doesNotHaveEmptyValues());
        var json = customer.toString();
        var deserialized = CustomerDto.fromJson(json);
        assertThat(deserialized, is(equalTo(customer)));
        assertThat(deserialized, doesNotHaveEmptyValues());
    }


    @Test
    void shouldThrowBadRequestExceptionWhenFailingToDeserialize(){
        String invalidJson = randomString();
        Executable action = () -> CustomerDto.fromJson(invalidJson);
        var exception=assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(),containsString(invalidJson));
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
            .withFeideOrganizationId(randomString())
            .withCristinId(randomString())
            .withCreatedDate(randomInstant().toString())
            .withModifiedDate(randomInstant().toString())
            .withVocabularies(randomVocabularies())
            .build();
    }

    private Collection<VocabularyDto> randomVocabularies() {
        return List.of(randomVocabulary(), randomVocabulary(), randomVocabulary());
    }


}