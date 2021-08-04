Feature: Update an existing Customer

  Scenario: The Administrator updates a Customer
    Given the Administrator wants to update an existing Customer and has a valid <Customer UUID>
    When they set the Content-Type header to "application/json"
    And they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they set the request body to a JSON object describing the Customer
    And they request PUT /customer/<Customer UUUID>
    Then they receive a response with status code 200
    And they see that the response Content-type is "application/json"
    And they see the response body contains a JSON object describing the Customer

  Scenario: The User updates a Customer
    Given the User wants to update an existing Customer and has valid <Customer UUID>
    When they set the Content-Type header to "application/json"
    And they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they set the request body to a JSON object describing the Customer
    And they request PUT /customer/<Customer UUUID>
    Then they receive a response with status code 401
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Unauthorized"
    And they see the response body has a field "status" with the value "401"

  Scenario: The persistence service is unavailable
    Given the Administrator wants to update an existing Customer and has a valid <Customer UUID>
    And the persistence service i unavailable
    When they set the Content-Type header to "application/json"
    And they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request GET /customer/<Customer UUID>
    Then they receive a response with status code 502
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Bad gateway"
    And they see the response body has a field "status" with the value "502"
    And they see the response body has a field "detail" with the value "Persistence service is unavailable"

  Scenario: The Administrator updates a badly formatted Customer
    Given the Administrator wants to update an existing Customer and has a valid <Customer UUID>
    When they set the Content-Type header to "application/json"
    And they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they set the request body to a JSON object describing the malformed Customer
    And they request POST /customer
    Then they receive a response with status code 400
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Bad request"
    And they see the response body has a field "status" with the value "400"

  Scenario Outline: The Administrator requests a badly formatted Customer UUID
    Given that the Administrator has a badly formatted <Customer UUID>
    When they set the Content-Type header to "application/json"
    And they set the Accept header to "application/json"
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