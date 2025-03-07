package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDto.ServiceCenter;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.VocabularyStatus;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.attempt.Try;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static no.unit.nva.customer.testing.CustomerDataGenerator.*;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CustomerBatchScanHandlerTest extends LocalCustomerServiceDatabase {

    private CustomerBatchScanHandler handler;
    private DynamoDBCustomerService service;
    private Context context;
    private Void input;

    @BeforeEach
    public void setUp() {
        super.setupDatabase();
        service = new DynamoDBCustomerService(this.dynamoClient);
        handler = new CustomerBatchScanHandler(service);
        context = new FakeContext();
    }

    @Test
    void shouldUpdateCustomers() {
        var existingCustomers = IntStream.of(1, randomInteger(10))
            .boxed()
            .map(i -> newCustomerDto())
            .map(attempt(c -> service.createCustomer(c)))
            .map(Try::orElseThrow)
            .map(CustomerDao::fromCustomerDto)
            .toList();

        final var logAppender = LogUtils.getTestingAppender(CustomerBatchScanHandler.class);
        handler.handleRequest(input, context);

        existingCustomers.forEach(customerDao -> {
            assertThat(logAppender.getMessages(), containsString(customerDao.getIdentifier().toString()));
        });
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
            .withGeneralSupportEnabled(true)
            .withChannelClaims(randomChannelClaimDtos())
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
}
