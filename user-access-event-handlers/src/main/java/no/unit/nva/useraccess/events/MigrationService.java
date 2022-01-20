package no.unit.nva.useraccess.events;

import no.unit.nva.useraccessmanagement.model.UserDto;

public interface MigrationService {


    UserDto migrateUserDto(UserDto userDto);

}
