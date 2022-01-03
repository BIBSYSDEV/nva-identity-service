package no.unit.nva.cognito.service;

import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.UserDto.Builder;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.core.attempt.Try;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.unit.nva.useraccessmanagement.model.ViewingScope.defaultViewingScope;
import static nva.commons.core.attempt.Try.attempt;

public class UserService {

    public static final String USER = "User";
    public static final String CREATOR = "Creator";
    public static final String FACULTY = "faculty";
    public static final String STAFF = "staff";
    private final UserApi userApi;

    public UserService(UserApi userApi) {
        this.userApi = userApi;
    }

    public Optional<UserDto> getUser(String feideId) {
        return userApi.getUser(feideId);
    }

    public UserDto createUser(UserDetails userDetails) {
        UserDto userInstance = createUserInstance(userDetails);
        return userApi.createUser(userInstance);
    }

    public UserDto updateUser(UserDto existingUser, UserDetails detailsUpdate) {
        List<RoleDto> updatedRoles = updateRoles(existingUser, detailsUpdate);

        UserDto updatedUser = Try.of(existingUser)
                                  .map(UserDto::copy)
                                  .map(copy -> detailsUpdatedInEveryLogin(copy, detailsUpdate))
                                  .map(builder -> builder.withRoles(updatedRoles))
                                  .map(Builder::build)
                                  .orElseThrow();

        userApi.updateUser(updatedUser);
        return updatedUser;
    }

    private List<RoleDto> updateRoles(UserDto existingUser, UserDetails detailsUpdate) {
        Set<String> automaticallyAssignedRolesForUser = createRolesFromAffiliation(detailsUpdate.getAffiliation());
        Set<String> rolesToBeRemoved = rolesToBeRemovedFromUser(automaticallyAssignedRolesForUser);
        Set<RoleDto> rolesToBeAdded = rolesToBeAddedToUser(existingUser, automaticallyAssignedRolesForUser);

        List<RoleDto> rolesToBeRetained = rolesToBeRetained(existingUser.getRoles(), rolesToBeRemoved);

        return Stream.concat(rolesToBeRetained.stream(), rolesToBeAdded.stream()).collect(Collectors.toList());
    }

    private List<RoleDto> rolesToBeRetained(Collection<RoleDto> existingRoles, Set<String> rolesToBeRemoved) {
        return existingRoles
                   .stream()
                   .filter(role -> !rolesToBeRemoved.contains(role.getRoleName()))
                   .collect(Collectors.toList());
    }

    private Set<String> rolesToBeRemovedFromUser(Set<String> automaticallyAssignedRolesToUser) {
        Set<String> rolesToBeRemoved = new HashSet<>(allAutomaticallyAssignedRoles());
        rolesToBeRemoved.removeAll(automaticallyAssignedRolesToUser);
        return Collections.unmodifiableSet(rolesToBeRemoved);
    }

    private Set<RoleDto> rolesToBeAddedToUser(UserDto existingUser, Set<String> rolesAutomaticallyAssignedToUser) {
        Set<String> rolesUserAlreadyHas = existingUser.getRoles()
                                              .stream()
                                              .map(RoleDto::getRoleName)
                                              .collect(Collectors.toSet());

        Set<String> rolesToBeAdded = new HashSet<>(rolesAutomaticallyAssignedToUser);

        rolesToBeAdded.removeAll(rolesUserAlreadyHas);

        return rolesToBeAdded.stream()
                   .map(attempt(name -> RoleDto.newBuilder().withName(name).build()))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toSet());
    }

    private Set<String> allAutomaticallyAssignedRoles() {
        return Stream.of(CREATOR).collect(Collectors.toUnmodifiableSet());
    }

    private UserDto createUserInstance(UserDetails userDetails) {
        return userDetails.getCustomerId()
                   .map(attempt(customerId -> createUserForInstitution(userDetails)))
                   .orElse(attempt(() -> createUserWithoutInstitution(userDetails)))
                   .orElseThrow();
    }

    private UserDto createUserWithoutInstitution(UserDetails userDetails) {
        return UserDto.newBuilder().withUsername(userDetails.getFeideId())
                   .withGivenName(userDetails.getGivenName())
                   .withFamilyName(userDetails.getFamilyName())
                   .withRoles(Collections.singletonList(RoleDto.newBuilder().withName(USER).build()))
                   .build();
    }

    private UserDto createUserForInstitution(UserDetails userDetails)  {
        List<RoleDto> roles = createRolesFromAffiliation(userDetails.getAffiliation()).stream()
                                  .map(attempt(roleName -> RoleDto.newBuilder().withName(roleName).build()))
                                  .map(Try::orElseThrow)
                                  .collect(Collectors.toList());

        roles.add(RoleDto.newBuilder().withName(USER).build());

        UserDto.Builder userBuilder = detailsUpdatedInEveryLogin(UserDto.newBuilder(), userDetails)
                                          .withUsername(userDetails.getFeideId())
                                          .withRoles(roles);

        calculateViewingScope(userDetails, userBuilder);

        return userBuilder.build();
    }

    private void calculateViewingScope(UserDetails userDetails, Builder userBuilder) {
        Optional<String> cristinId = userDetails.getCristinId();
        if (cristinId.isPresent()) {
            ViewingScope viewingScope = defaultViewingScope(URI.create(cristinId.get()));
            userBuilder.withViewingScope(viewingScope);
        }
    }

    private UserDto.Builder detailsUpdatedInEveryLogin(UserDto.Builder userBuilder, UserDetails userDetails) {
        return userBuilder
                   .withFamilyName(userDetails.getFamilyName())
                   .withGivenName(userDetails.getGivenName())
                   .withInstitution(userDetails.getCustomerId().orElse(null));
    }

    /**
     * Create user roles from users give affiliation at organization.
     *
     * @param affiliation affiliation
     * @return list of roles
     * @see <a href="https://www.feide.no/attribute/edupersonaffiliation">Feide eduPersonAffiliation</a>
     */
    private Set<String> createRolesFromAffiliation(final String affiliation) {
        String lowerCaseAffiliation = affiliation.toLowerCase(Locale.getDefault());
        Set<String> roles = new HashSet<>();

        if (lowerCaseAffiliation.contains(STAFF) || lowerCaseAffiliation.contains(FACULTY)) {
            roles.add(CREATOR);
        }

        return Collections.unmodifiableSet(roles);
    }
}
