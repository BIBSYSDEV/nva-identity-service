Feature: Get Customer

  Scenario Outline: The Administrator requests a Customer
    Given that the Administrator has a valid <Customer UUID>
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request GET /customer/<Customer UUID>
    Then they receive a response with status code 200
    And they see that the response Content-Type header is "application/json"
    And they see that the response body is a JSON object describing the Customer
    And they see that the Customer has an UUID
    And they see that the Customer has a name
    And they see that the Customer has a display name
    And they see that the Customer has an archive name
    And they see that the Customer has a CNAME
    And they see that the Customer has an Institution DNS
    And they see that the Customer has an Administration ID
    And they see that the Customer has a Feide Organization ID
    And they see that the Customer has a created date
    And they see that the Customer has a modified date
    And they see that the Customer has a contact
    And they see that the Customer has a logo file

    Examples:
      | Customer UUID |
      | 84c28180-6cb3-4512-9674-55b3e426bf7e |
      | 40fd8244-9601-414c-b24c-47acecba501a |

  Scenario: The Administrator requests a Customer that does not exist
    Given that the Administrator has an invalid <Customer UUID>
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request GET /customer/<Customer UUID>
    Then they receive a response with status code 404
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Not found"
    And they see the response body has a field "status" with the value "404"

  Scenario: The persistence service is unavailable
    Given that the Administrator requests a Customer but the persistence service i unavailable
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request GET /customer/<Customer UUID>
    Then they receive a response with status code 502
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Bad gateway"
    And they see the response body has a field "status" with the value "502"
    And they see the response body has a field "detail" with the value "Persistence service is unavailable"

  Scenario Outline: The Administrator requests a badly formatted Customer UUID
    Given that the Administrator has a badly formatted <Customer UUID>
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request GET /customer/<Customer UUID>
    Then they receive a response with status code 400
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Bad request"
    And they see the response body has a field "status" with the value "400"
    And they see the response body has a field "detail" with the value "The request contained a malformed UUID"

    Examples:
      | Customer UUID |
      | 123 |
      | abc |