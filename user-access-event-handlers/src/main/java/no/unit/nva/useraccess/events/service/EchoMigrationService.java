package no.unit.nva.useraccess.events.service;

import no.unit.nva.useraccessmanagement.model.UserDto;

public class EchoMigrationService implements UserMigrationService {

    @Override
    public UserDto migrateUser(UserDto user) {
        return user;
    }
}
