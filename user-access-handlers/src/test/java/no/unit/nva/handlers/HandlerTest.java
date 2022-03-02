package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.jr.ob.JSON;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import no.unit.nva.database.DatabaseAccessor;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigatewayv2.exceptions.ConflictException;

public class HandlerTest extends DatabaseAccessor {

    public static final String DEFAULT_USERNAME = "someUsername@inst";
    public static final String DEFAULT_ROLE = "SomeRole";
    public static final URI DEFAULT_INSTITUTION = randomCristinOrgId();
    public static final String TYPE_ATTRIBUTE = "type";
    private static final String SPECIAL_CHARACTER = "@";
    private static final String ENCODED_SPECIAL_CHARACTER = "%40";


    protected UserDto insertSampleUserToDatabase(String username, URI institution)
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        UserDto sampleUser = createSampleUserWithExistingRoles(username, institution);
        databaseService.addUser(sampleUser);
        return sampleUser;
    }

    protected UserDto insertSampleUserToDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        return insertSampleUserToDatabase(DEFAULT_USERNAME, DEFAULT_INSTITUTION);
    }

    protected UserDto createSampleUserWithExistingRoles(String username, URI institution)
        throws InvalidEntryInternalException {
        UserDto sampleUser = createSampleUser(username, institution);
        sampleUser.getRoles().forEach((this::insertRole));
        return sampleUser;
    }

    protected UserDto createSampleUserWithExistingRoles() throws InvalidEntryInternalException {
        return createSampleUserWithExistingRoles(DEFAULT_USERNAME, DEFAULT_INSTITUTION);
    }

    protected UserDto createSampleUser(String username, URI institution) throws InvalidEntryInternalException {
        RoleDto someRole = RoleDto.newBuilder().withRoleName(DEFAULT_ROLE).build();
        return UserDto.newBuilder()
            .withUsername(username)
            .withRoles(Collections.singletonList(someRole))
            .withInstitution(institution)
            .build();
    }

    protected <T> APIGatewayProxyRequestEvent createRequestInputStream(T bodyObject)
        throws JsonProcessingException {
        var bodyString = attempt(()->JSON.std.asString(bodyObject)).orElseThrow();
        return  new APIGatewayProxyRequestEvent().withBody(bodyString);

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
