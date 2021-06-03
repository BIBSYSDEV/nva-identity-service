package no.unit.nva.cognito.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import no.unit.nva.useraccessmanagement.model.UserDto;

public interface UserApi {

    Optional<UserDto> getUser(String username);

    UserDto createUser(UserDto user);

    void updateUser(UserDto user) throws URISyntaxException, IOException,  InterruptedException;
}
