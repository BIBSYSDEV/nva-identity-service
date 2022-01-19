package no.unit.nva.useraccess.events;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartBatchScanTest {

    public static final Context CONTEXT = new FakeContext() {
        @Override
        public String getInvokedFunctionArn() {
            return randomString();
        }
    };
    private StartBatchScan batchScanner;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        batchScanner = new StartBatchScan(new FakeEventBridgeClient());
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldSendEventContainingTheEventTypeAndNullStartingPointWhenInputIsAnEmptyObject()
        throws IOException {
        batchScanner.handleRequest(emptyInput(), outputStream, CONTEXT);
        var actualEntry = ScanDatabaseRequest.fromJson(outputStream.toString());
        var expectedEntry = new ScanDatabaseRequest(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC,
                                                    ScanDatabaseRequest.DEFAULT_PAGE_SIZE,
                                                    null);
        assertThat(actualEntry, is(equalTo(expectedEntry)));
    }

    private InputStream emptyInput() {
        return IoUtils.stringToStream(EventsConfig.objectMapper.createObjectNode().toString());
    }
}