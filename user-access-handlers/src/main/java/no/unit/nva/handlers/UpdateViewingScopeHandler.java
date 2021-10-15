package no.unit.nva.handlers;

import no.unit.nva.database.DatabaseService;

public class UpdateViewingScopeHandler {

    private final DatabaseService databaseService;

    public UpdateViewingScopeHandler(DatabaseService databaseService) {

        this.databaseService = databaseService;
    }
}
