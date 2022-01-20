package no.unit.nva.useraccess.events.service;

import no.unit.nva.useraccessmanagement.dao.UserDb;

public interface UserMigrationService {

    UserDb migrateUser(UserDb user);

}
