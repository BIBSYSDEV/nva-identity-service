package no.unit.nva.cognito.service;

import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import no.unit.nva.cognito.exception.UserServiceException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;

public class UserApiMock implements UserApi {

    public static final String FIRST_ACCESS_RIGHT = "SAMPLE_ACCESS_RIGHT_1";
    public static final String SECOND_ACCESS_RIGHT = "SAMPLE_ACCESS_RIGHT_2";
    public static final Set<String> SAMPLE_ACCESS_RIGHTS = Set.of(FIRST_ACCESS_RIGHT, SECOND_ACCESS_RIGHT);
    public static final String CREATE_USER_CALL = "createUser";
    public static final String GET_USER_CALL = "getUser";
    public static final String UPDATE_USER_CALL = "updateUser";
    private UserDto user;

    private Map<String, AtomicInteger> methodCalls = initCounters();
    private UserDto userUpdate;

    @Override
    public Optional<UserDto> getUser(String username) {
        methodCalls.get(GET_USER_CALL).incrementAndGet();
        return Optional.ofNullable(user);
    }

    @Override
    public UserDto createUser(UserDto user) {
        methodCalls.get(CREATE_USER_CALL).incrementAndGet();
        UserDto updatedUser = updateRolesWithAccessRights(user);
        this.user = updatedUser;

        if (user != null) {
            return user;
        } else {
            throw new UserServiceException(UserDbClient.ERROR_CREATING_USER);
        }
    }

    @Override
    public void updateUser(UserDto user) {
        methodCalls.get(UPDATE_USER_CALL).incrementAndGet();
        this.user = user;
    }

    public int getUpdateCalls() {
        return methodCalls.get(UPDATE_USER_CALL).intValue();
    }

    public UserDto getUserUpdate() {
        return userUpdate;
    }

    private ConcurrentHashMap<String, AtomicInteger> initCounters() {
        ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>();
        map.put(CREATE_USER_CALL, new AtomicInteger(0));
        map.put(GET_USER_CALL, new AtomicInteger(0));
        map.put(UPDATE_USER_CALL, new AtomicInteger(0));
        return map;
    }

    private UserDto updateRolesWithAccessRights(UserDto user) {
        Set<RoleDto> updatedRoles = updateRoles(user.getRoles());
        return attempt(() -> user.copy().withRoles(updatedRoles).build()).toOptional().orElseThrow();
    }

    private Set<RoleDto> updateRoles(Set<RoleDto> roles) {
        return roles.stream().map(this::addAccessRights).collect(Collectors.toSet());
    }

    private RoleDto addAccessRights(RoleDto role) {
        return attempt(() -> role.copy().withAccessRights(SAMPLE_ACCESS_RIGHTS).build())
                   .toOptional().orElseThrow();
    }
}