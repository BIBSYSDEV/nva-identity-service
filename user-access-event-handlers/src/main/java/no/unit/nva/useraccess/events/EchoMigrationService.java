package no.unit.nva.useraccess.events;

import java.util.ArrayList;
import java.util.List;
import no.unit.nva.useraccessmanagement.model.UserDto;

public class EchoMigrationService implements MigrationService {

    private final List<UserDto> scannedUsers = new ArrayList<>();

    public List<UserDto> getScannedUsers() {
        return scannedUsers;
    }

    @Override
    public UserDto migrateUserDto(UserDto userDto) {
        scannedUsers.add(userDto);
        return userDto;
    }
}
