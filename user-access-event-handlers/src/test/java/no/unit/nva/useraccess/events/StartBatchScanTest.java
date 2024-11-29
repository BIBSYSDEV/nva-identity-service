package no.unit.nva.useraccess.events;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class StartBatchScanTest {

    public static final Context CONTEXT = new FakeContext() {
        @Override
        public String getInvokedFunctionArn() {
            return randomString();
        }
    };
    private StartBatchScan batchScanner;
    private ByteArrayOutputStream outputStream;
    private FakeEventBridgeClient eventClient;

    @BeforeEach
    public void init() {
        eventClient = new FakeEventBridgeClient();
        batchScanner = new StartBatchScan(eventClient);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldSendEventContainingTheEventTypeForBatchScanningAndNullStartingPointWhenInputIsAnEmptyObject()
        throws IOException {
        batchScanner.handleRequest(emptyInput(), outputStream, CONTEXT);
        var emittedEventBody = extractEventBody();
        var expectedEmittedEventBody = new ScanDatabaseRequestV2(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC,
            ScanDatabaseRequest.DEFAULT_PAGE_SIZE,
            null);
        assertThat(emittedEventBody, is(equalTo(expectedEmittedEventBody)));
    }

    private ScanDatabaseRequestV2 extractEventBody() {
        return eventClient.getRequestEntries().stream()
            .map(PutEventsRequestEntry::detail)
            .map(attempt(ScanDatabaseRequestV2::fromJson))
            .map(Try::orElseThrow)
            .collect(SingletonCollector.collect());
    }

    private InputStream emptyInput() throws IOException {
        return IoUtils.stringToStream(JsonConfig.writeValueAsString(Collections.emptyMap()));
    }

    @Test
    void shouldSendEventToEventBusDefinedInTheEnvironment()
        throws IOException {
        batchScanner.handleRequest(emptyInput(), outputStream, CONTEXT);
        var actualEventBus = extractEventBus();
        var expectedEventBus = EventsConfig.EVENT_BUS;
        assertThat(actualEventBus, is(equalTo(expectedEventBus)));
    }

    private String extractEventBus() {
        return eventClient.getRequestEntries().stream()
            .map(PutEventsRequestEntry::eventBusName)
            .collect(SingletonCollector.collect());
    }
}