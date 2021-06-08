Feature: Get all Customers

  Scenario: The Administrator requests all Customers
    Given that the Administrator is looking for all Customers
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request GET /customer
    Then they receive a response with status code 200
    And they see that the response Content-Type header is "application/json"
    And they see that the response body is a JSON object with a list of Customers

  Scenario: No Customers exist
    Given that the Administrator is looking for all Customers
    And no Customers exist
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request GET /customer
    Then they receive a response with status code 200
    And they see that the response Content-Type header is "application/json"
    And they see that the response body is a JSON object with an empty Customers list

  Scenario: The persistence service is unavailable
    Given that the Administrator is looking for all Customer
    And the persistence service i unavailable
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request GET /customer
    Then they receive a response with status code 502
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Bad gateway"
    And they see the response body has a field "status" with the value "502"
    And they see the response body has a field "detail" with the value "Persistence service is unavailable"
