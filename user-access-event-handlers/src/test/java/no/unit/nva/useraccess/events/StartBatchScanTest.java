package no.unit.nva.useraccess.events;

import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.models.ScanDatabaseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartBatchScanTest {

    public static final Context CONTEXT = mock(Context.class);
    private StartBatchScan batchScanner;

    @BeforeEach
    public void init() {
        batchScanner = new StartBatchScan();
    }

    @Test
    void shouldSendEventContainingTheEventTypeAndNullStartingPointWhenInputIsAnEmptyObject() {
        var actualEntry = batchScanner.handleRequest(emptyInput(), CONTEXT);
        var expectedEntry = new ScanDatabaseRequest(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC,
                                                    ScanDatabaseRequest.DEFAULT_PAGE_SIZE,
                                                    null);
        assertThat(actualEntry, is(equalTo(expectedEntry)));
    }

    private ScanDatabaseRequest emptyInput() {
        return new ScanDatabaseRequest(null, null, null);
    }
}