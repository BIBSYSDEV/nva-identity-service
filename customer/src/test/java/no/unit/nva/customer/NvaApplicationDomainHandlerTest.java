package no.unit.nva.customer;

import static no.unit.nva.customer.testing.CustomerDataGenerator.randomCristinOrgId;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomAllowFileUploadForTypes;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomPublicationWorkflow;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomDoiAgent;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomRightsRetentionStrategy;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomSector;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDto.ServiceCenter;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.VocabularyStatus;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NvaApplicationDomainHandlerTest extends LocalCustomerServiceDatabase {

    private NvaApplicationDomainHandler handler;
    private DynamoDBCustomerService service;
    private Context context;
    private Void input;

    @BeforeEach
    public void setUp() {
        super.setupDatabase();
        service = new DynamoDBCustomerService(this.dynamoClient);
        handler = new NvaApplicationDomainHandler(service);
        context = new FakeContext();
    }

    @Test
    void shouldUpdateAttributeCustomerOf() {
        var expectedCustomersAppDomains = IntStream.of(1, randomInteger(10))
                                              .boxed()
                                              .map(i -> newCustomerDto())
                                              .map(attempt(c -> service.createCustomer(c)))
                                              .map(Try::orElseThrow)
                                              .map(customer -> updateCustomerOfNvaAttribute(customer).getCustomerOf())
                                              .collect(Collectors.toList());
    
        var actualCustomersAppDomains = handler.handleRequest(input, context)
                                            .stream()
                                            .map(CustomerDto::getCustomerOf)
                                            .collect(Collectors.toList());

        assertThat(expectedCustomersAppDomains, is(equalTo(actualCustomersAppDomains)));
    }

    private CustomerDto newCustomerDto() {
        var oneMinuteInThePast = Instant.now().minusSeconds(60L);
        var customer = CustomerDto.builder()
                           .withName(randomString())
                           .withShortName(randomString())
                           .withCreatedDate(oneMinuteInThePast)
                           .withModifiedDate(oneMinuteInThePast)
                           .withDisplayName(randomString())
                           .withArchiveName(randomString())
                           .withCname(randomString())
                           .withInstitutionDns(randomString())
                           .withFeideOrganizationDomain(randomString())
                           .withCristinId(randomCristinOrgId())
                           .withCustomerOf(ApplicationDomain.fromUri(URI.create("")))
                           .withVocabularies(randomVocabularySet())
                           .withRorId(randomUri())
                           .withServiceCenter(new ServiceCenter(randomUri(), randomString()))
                           .withPublicationWorkflow(randomPublicationWorkflow())
                           .withDoiAgent(randomDoiAgent(randomString()))
                           .withSector(randomSector())
                           .withNviInstitution(randomBoolean())
                           .withRboInstitution(randomBoolean())
                           .withInactiveFrom(randomInstant())
                           .withAllowFileUploadForTypes(randomAllowFileUploadForTypes())
                           .withRightsRetentionStrategy(randomRightsRetentionStrategy())
                           .build();
        assertThat(customer, doesNotHaveEmptyValuesIgnoringFields(Set.of("identifier", "id", "context",
                                                                         "doiAgent.password", "doiAgent.id")));
        return customer;
    }

    private Set<VocabularyDto> randomVocabularySet() {
        return Set.of(randomVocabulary(), randomVocabulary());
    }

    private VocabularyDto randomVocabulary() {
        return new VocabularyDto(randomString(), randomUri(), randomElement(VocabularyStatus.values()));
    }

    private CustomerDto updateCustomerOfNvaAttribute(CustomerDto customerDto) {
        customerDto.setCustomerOf(ApplicationDomain.NVA);
        return customerDto;
    }
}
