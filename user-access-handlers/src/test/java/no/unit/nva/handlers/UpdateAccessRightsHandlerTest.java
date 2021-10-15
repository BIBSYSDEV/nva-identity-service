package no.unit.nva.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateAccessRightsHandlerTest extends HandlerTest {

    private UpdateViewingScopeHandler handler;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        handler = new UpdateViewingScopeHandler(databaseService);
    }

    @Test
    void OkWhenInputRequestUpdated


}
