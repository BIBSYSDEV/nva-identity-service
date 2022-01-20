package no.unit.nva.useraccess.events;

import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventBasedScanHandlerTest {

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldAcceptAndLogAnEvent() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        EventBasedScanHandler eventBasedScanHandler = new EventBasedScanHandler();
        eventBasedScanHandler.handleRequest(sampleEventWithoutStartingPointer(), outputStream, mock(Context.class));
        assertThat(appender.getMessages(), containsString(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC));
    }

    private InputStream sampleEventWithoutStartingPointer() {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(createSampleScanRequest());
    }

    private ScanDatabaseRequest createSampleScanRequest() {
        return new ScanDatabaseRequest(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC, null, null);
    }
}