package no.unit.nva.useraccess.events.service;

import no.unit.nva.useraccessmanagement.model.UserDto;

public interface UserMigrationService {

    UserDto migrateUser(UserDto user);
}
