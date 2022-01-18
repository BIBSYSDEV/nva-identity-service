package no.unit.nva.useraccess.events;

import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchScannerTest {

    public static final Context CONTEXT = mock(Context.class);
    private BatchScanner batchScanner;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        batchScanner = new BatchScanner();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldSendEventContainingTheEventTypeAndNullStartingPoint() {
        batchScanner.handleRequest(emptyInput(), outputStream, CONTEXT);
    }

    private InputStream emptyInput() {
        var emptyNode = JsonUtils.dtoObjectMapper.createObjectNode().toString();
        return IoUtils.stringToStream(emptyNode);
    }
}