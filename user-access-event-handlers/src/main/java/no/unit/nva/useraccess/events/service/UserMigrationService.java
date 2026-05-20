package no.unit.nva.useraccess.events.service;

import no.unit.nva.useraccessservice.model.UserDto;

@FunctionalInterface
public interface UserMigrationService {

    UserDto migrateUser(UserDto user, String action);
}
