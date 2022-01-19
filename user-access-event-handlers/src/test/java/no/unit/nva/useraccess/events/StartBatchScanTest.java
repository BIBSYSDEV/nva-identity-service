package no.unit.nva.useraccess.events;

import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.events.models.ScanDatabaseRequest;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartBatchScanTest {

    public static final Context CONTEXT = mock(Context.class);
    private StartBatchScan batchScanner;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        batchScanner = new StartBatchScan();
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
        var request = new ScanDatabaseRequest(null, null, null);
        return IoUtils.stringToStream(request.toJsonString());
    }
}