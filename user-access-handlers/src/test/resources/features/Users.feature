Feature: Users

  Background:


    Given a Database for users and roles
    And an AuthorizedClient that is authorized through Feide
    And an ExistingRole with role-name "RoleA" that exists in the Database

    And an ExistingUser with username "existingUser" that exists in the Database
    And the ExistingUser belongs to the institution "StandardInstitution"
    And the ExistingUser has the roles:
      | roles |
      | RoleA |

    And a NonExistingUser with username "nonExistingUser" that does not exist in the database

    And a non-existing NewUser with username "newUser" that does not yet exist in the database
    And NewUser is specified that it will have the roles:
      | roles |
      | RoleA |

  Scenario: AuthorizedClient adds a new user
    When the AuthorizedClient sends a request to add the NewUser to the Database
    Then the NewUser is added to the database
    Then in the response, the object is the UserDescription of the NewUser

  Scenario: Authorized client gets an existing user
    When the AuthorizedClient sends a request to read the ExistingUser from the Database
    Then in the response, the object is the UserDescription of the ExistingUser

  Scenario: Authorized client gets a non-existing user
    When the AuthorizedClient sends a request to read the NonExistingUser from the Database
    Then a NotFound message is returned

  Scenario:  Authorized client updates existing user
    Given a NewRole with role-name "roleAfterUpdate" that exists in the Database
    When the AuthorizedClient requests to update the ExistingUser setting the following roles:
      | roles           |
      | roleAfterUpdate |

    Then the ExistingUser is updated asynchronously
    And the response has a Location header with ExistingUser's URI as value
    And the ExistingUser contains only the following roles:
      | roles           |
      | roleAfterUpdate |

  Scenario: Authorized client attempts to update non-existing user
    When the AuthorizedClient requests to update the NonExistingUser
    Then a NotFound message is returned


  Scenario:Authorized client attempts to update existing user with malformed request
    When the AuthorizedClient requests to update the ExistingUser using a malformed body
    Then a BadRequest message is returned containing information about the invalid request


  Scenario: AuthorizedClient requests list of users of specified institution
    Given a UserA with username "userA" that exists in the Database
    And the UserA belongs to the institution "Institution"

    And a UserB with username "userB" that exists in the Database
    And the UserB belongs to the institution "Institution"

    And a UserC with username "userC" that exists in the Database
    And the UserC belongs to the institution "AnotherInstitution"

    When the AuthorizedClient sends the request to list the users of the "Institution"
    Then a non-empty list of the users belonging to the institution is returned
    And the list of users should contain only the following usernames:
      | usernames |
      | userA     |
      | userB     |




