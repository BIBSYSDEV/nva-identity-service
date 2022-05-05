package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;

public class HandlerTest extends LocalIdentityService {

    public static final String DEFAULT_USERNAME = "someUsername@inst";
    public static final URI DEFAULT_INSTITUTION = randomCristinOrgId();
    public static final String TYPE_ATTRIBUTE = "type";
    private static final String SPECIAL_CHARACTER = "@";
    private static final String ENCODED_SPECIAL_CHARACTER = "%40";

    protected UserDto insertSampleUserToDatabase(String username, URI institution)
        throws InvalidEntryInternalException, ConflictException {
        UserDto sampleUser = createSampleUserAndInsertUserRoles(username, institution);
        databaseService.addUser(sampleUser);
        return sampleUser;
    }

    protected UserDto insertSampleUserToDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        return insertSampleUserToDatabase(DEFAULT_USERNAME, DEFAULT_INSTITUTION);
    }

    protected UserDto createSampleUserAndInsertUserRoles(String username, URI institution)
        throws InvalidEntryInternalException {
        UserDto sampleUser = createSampleUser(username, institution);
        sampleUser.getRoles().forEach((this::insertRole));
        return sampleUser;
    }

    protected UserDto createSampleUserAndInsertUserRoles() throws InvalidEntryInternalException {
        return createSampleUserAndInsertUserRoles(DEFAULT_USERNAME, DEFAULT_INSTITUTION);
    }

    protected UserDto createSampleUser(String username, URI institution) throws InvalidEntryInternalException {
        RoleDto someRole = RoleDto.newBuilder().withRoleName(randomString()).build();
        return UserDto.newBuilder()
            .withUsername(username)
            .withRoles(Collections.singletonList(someRole))
            .withInstitution(institution)
            .build();
    }


    protected String encodeString(String inputContainingSpecialCharacter) {
        assertThat(inputContainingSpecialCharacter, containsString(SPECIAL_CHARACTER));
        String output = URLEncoder.encode(inputContainingSpecialCharacter, StandardCharsets.UTF_8);
        assertThat(output, containsString(ENCODED_SPECIAL_CHARACTER));
        return output;
    }

    private void insertRole(RoleDto role) {
        try {
            databaseService.addRole(role);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
