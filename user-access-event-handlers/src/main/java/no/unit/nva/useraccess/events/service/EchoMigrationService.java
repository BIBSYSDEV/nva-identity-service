package no.unit.nva.useraccess.events.service;

import java.util.ArrayList;
import java.util.List;
import no.unit.nva.useraccessmanagement.model.UserDto;

public class EchoMigrationService implements UserMigrationService {

    private final List<UserDto> scannedUsers = new ArrayList<>();

    public List<UserDto> getScannedUsers() {
        return scannedUsers;
    }

    @Override
    public UserDto migrateUser(UserDto user) {
        scannedUsers.add(user);
        return user;
    }
}
