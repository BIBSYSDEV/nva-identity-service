package no.unit.nva.handlers;

import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomRoleNameButNot;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

public class HandlerTest extends LocalIdentityService {

    public static final String DEFAULT_USERNAME = "someUsername@inst";
    public static final URI DEFAULT_INSTITUTION = randomCristinOrgId();
    public static final String TYPE_ATTRIBUTE = "type";
    private static final String SPECIAL_CHARACTER = "@";
    private static final String ENCODED_SPECIAL_CHARACTER = "%40";
    private static final Logger logger = LoggerFactory.getLogger(HandlerTest.class);

    protected UserDto insertSampleUserToDatabase(String username, URI institution, RoleName roleName)
        throws InvalidEntryInternalException, ConflictException {
        UserDto sampleUser = createSampleUserAndInsertUserRoles(username, institution, roleName);
        databaseService.addUser(sampleUser);
        return sampleUser;
    }

    protected UserDto createSampleUserAndInsertUserRoles(String username, URI institution, RoleName roleName)
        throws InvalidEntryInternalException {
        UserDto sampleUser = createSampleUser(username, institution, roleName);
        sampleUser.getRoles().forEach((this::insertRole));
        return sampleUser;
    }

    protected UserDto createSampleUser(String username, URI institution, RoleName roleName)
        throws InvalidEntryInternalException {
        RoleDto someRole = RoleDto.newBuilder().withRoleName(roleName).build();
        return UserDto.newBuilder()
            .withUsername(username)
            .withRoles(Collections.singletonList(someRole))
            .withInstitution(institution)
            .build();
    }

    private void insertRole(RoleDto role) {
        try {
            var existingRole = attempt(() -> databaseService.getRole(role)).toOptional();
            if (existingRole.isEmpty()) {
                databaseService.addRole(role);
            }
        } catch (Exception e) {
            logger.error("Error inserting role", e);
        }
    }

    protected UserDto insertSampleUserToDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        return insertSampleUserToDatabase(DEFAULT_USERNAME, DEFAULT_INSTITUTION);
    }

    protected UserDto insertSampleUserToDatabase(String username, URI institution)
        throws InvalidEntryInternalException, ConflictException {
        UserDto sampleUser = createSampleUserAndInsertUserRoles(username, institution);
        databaseService.addUser(sampleUser);
        return sampleUser;
    }

    protected UserDto createSampleUserAndInsertUserRoles(String username, URI institution)
        throws InvalidEntryInternalException {
        UserDto sampleUser = createSampleUser(username, institution);
        sampleUser.getRoles().forEach((this::insertRole));
        return sampleUser;
    }

    protected UserDto createSampleUser(String username, URI institution) throws InvalidEntryInternalException {
        RoleDto someRole = RoleDto.newBuilder().withRoleName(randomRoleNameButNot(RoleName.APPLICATION_ADMIN)).build();
        return UserDto.newBuilder()
            .withUsername(username)
            .withRoles(Collections.singletonList(someRole))
            .withInstitution(institution)
            .build();
    }

    protected UserDto createSampleUserAndInsertUserRoles() throws InvalidEntryInternalException {
        return createSampleUserAndInsertUserRoles(DEFAULT_USERNAME, DEFAULT_INSTITUTION);
    }

    protected ClientDto insertClientToDatabase(ClientDto clientDto)
        throws InvalidEntryInternalException {
        databaseService.addExternalClient(clientDto);
        return clientDto;
    }

    protected String encodeString(String inputContainingSpecialCharacter) {
        assertThat(inputContainingSpecialCharacter, containsString(SPECIAL_CHARACTER));
        String output = URLEncoder.encode(inputContainingSpecialCharacter, StandardCharsets.UTF_8);
        assertThat(output, containsString(ENCODED_SPECIAL_CHARACTER));
        return output;
    }
}
