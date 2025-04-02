package no.unit.nva.useraccess.events.service;

import no.unit.nva.useraccessservice.model.UserDto;

public interface UserMigrationService {

    UserDto migrateUser(UserDto user, String action);
}
