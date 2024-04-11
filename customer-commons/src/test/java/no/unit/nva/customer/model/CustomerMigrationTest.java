package no.unit.nva.customer.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CustomerMigrationTest {

    public static Stream<Arguments> serviceCenterProvider() {
        return Stream.of(Arguments.of(customerWithServiceCenterUri()),
                         Arguments.of(customerWithServiceCenterObject()),
                         Arguments.of(customerWithEmptyServiceCenter()),
                         Arguments.of(customerWithMissingServiceCenter()),
                         Arguments.of(customerWithNullServiceCenter())
        );
    }

    @ParameterizedTest
    @MethodSource("serviceCenterProvider")
    void shouldMigrateServiceCenterUriToObject(String customer) throws JsonProcessingException {
        var customerDao = JsonUtils.dtoObjectMapper.readValue(customer, CustomerDao.class);
        assertDoesNotThrow(customerDao::toCustomerDto);
    }

    private static String customerWithNullServiceCenter() {
        return """
                           {
              "id": "https://localhost/47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "identifier": "47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "createdDate": "2003-03-27T20:59:02.725Z",
              "modifiedDate": "2010-03-12T11:22:33.135Z",
              "name": "V1cyvCG8ChtM",
              "displayName": "9knaaGRSFr9gM0j",
              "shortName": "nmkPoRSQzc7vURa",
              "archiveName": "BYgxqwqfBjD17KgosJ",
              "cname": "LKjlM7dzZ3ZHw1hGu",
              "institutionDns": "2NwOzIEM2tTv",
              "feideOrganizationDomain": "b58v2c4tNmjwbC2",
              "cristinId": "https://www.example.com/E7nWFINRiV",
              "customerOf": "nva.unit.no",
              "serviceCenterUri": null,
              "vocabularies": [
                {
                  "type": "Vocabulary",
                  "name": "6q02lavfZkad",
                  "id": "https://www.example.com/3vWcHKglac",
                  "status": "Default"
                }
              ],
              "rorId": "https://www.example.com/9CCuwMlwrHA5",
              "nviInstitution": false,
              "rboInstitution": false,
              "allowFileUploadForTypes": [
                "ReportWorkingPaper"
              ],
              "type": "Customer"
            }
            """;
    }

    private static String customerWithMissingServiceCenter() {
        return """
                           {
              "id": "https://localhost/47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "identifier": "47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "createdDate": "2003-03-27T20:59:02.725Z",
              "modifiedDate": "2010-03-12T11:22:33.135Z",
              "name": "V1cyvCG8ChtM",
              "displayName": "9knaaGRSFr9gM0j",
              "shortName": "nmkPoRSQzc7vURa",
              "archiveName": "BYgxqwqfBjD17KgosJ",
              "cname": "LKjlM7dzZ3ZHw1hGu",
              "institutionDns": "2NwOzIEM2tTv",
              "feideOrganizationDomain": "b58v2c4tNmjwbC2",
              "cristinId": "https://www.example.com/E7nWFINRiV",
              "customerOf": "nva.unit.no",
              "vocabularies": [
                {
                  "type": "Vocabulary",
                  "name": "6q02lavfZkad",
                  "id": "https://www.example.com/3vWcHKglac",
                  "status": "Default"
                }
              ],
              "rorId": "https://www.example.com/9CCuwMlwrHA5",
              "nviInstitution": false,
              "rboInstitution": false,
              "allowFileUploadForTypes": [
                "ReportWorkingPaper"
              ],
              "type": "Customer"
            }
            """;
    }

    private static String customerWithEmptyServiceCenter() {
        return """
                           {
              "id": "https://localhost/47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "identifier": "47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "createdDate": "2003-03-27T20:59:02.725Z",
              "modifiedDate": "2010-03-12T11:22:33.135Z",
              "name": "V1cyvCG8ChtM",
              "displayName": "9knaaGRSFr9gM0j",
              "shortName": "nmkPoRSQzc7vURa",
              "archiveName": "BYgxqwqfBjD17KgosJ",
              "cname": "LKjlM7dzZ3ZHw1hGu",
              "institutionDns": "2NwOzIEM2tTv",
              "feideOrganizationDomain": "b58v2c4tNmjwbC2",
              "cristinId": "https://www.example.com/E7nWFINRiV",
              "customerOf": "nva.unit.no",
              "vocabularies": [
                {
                  "type": "Vocabulary",
                  "name": "6q02lavfZkad",
                  "id": "https://www.example.com/3vWcHKglac",
                  "status": "Default"
                }
              ],
              "rorId": "https://www.example.com/9CCuwMlwrHA5",
              "serviceCenter": {
              },
              "nviInstitution": false,
              "rboInstitution": false,
              "allowFileUploadForTypes": [
                "ReportWorkingPaper"
              ],
              "type": "Customer"
            }
            """;
    }

    private static String customerWithServiceCenterUri() {
        return """
                        {
              "id": "https://localhost/47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "identifier": "47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "createdDate": "2003-03-27T20:59:02.725Z",
              "modifiedDate": "2010-03-12T11:22:33.135Z",
              "name": "V1cyvCG8ChtM",
              "displayName": "9knaaGRSFr9gM0j",
              "shortName": "nmkPoRSQzc7vURa",
              "archiveName": "BYgxqwqfBjD17KgosJ",
              "cname": "LKjlM7dzZ3ZHw1hGu",
              "institutionDns": "2NwOzIEM2tTv",
              "feideOrganizationDomain": "b58v2c4tNmjwbC2",
              "cristinId": "https://www.example.com/E7nWFINRiV",
              "customerOf": "nva.unit.no",
              "vocabularies": [
                {
                  "type": "Vocabulary",
                  "name": "6q02lavfZkad",
                  "id": "https://www.example.com/3vWcHKglac",
                  "status": "Default"
                }
              ],
              "rorId": "https://www.example.com/9CCuwMlwrHA5",
              "serviceCenterUri": "https://www.example.com/vplpKxc5Iz8I",
              "publicationWorkflow": "RegistratorRequiresApprovalForMetadataAndFiles",
              "doiAgent": {
                "id": "https://localhost/47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd/doiagent",
                "url": "mds.ezInCEVzmxq.datacite.org",
                "prefix": "10.000",
                "username": "user-name-ezInCEVzmxq"
              },
              "nviInstitution": true,
              "rboInstitution": false,
              "inactiveFrom": "1999-04-25T15:32:09.317Z",
              "sector": "UHI",
              "rightsRetentionStrategy": {
                "type": "OverridableRightsRetentionStrategy",
                "id": "https://www.example.com/tGwu1dHQluKz"
              },
              "allowFileUploadForTypes": [
                "JournalCorrigendum",
                "NonFictionChapter",
                "BookAnthology"
              ],
              "@context": "https://bibsysdev.github.io/src/customer-context.json",
              "type": "Customer"
            }
            """;
    }

    private static String customerWithServiceCenterObject() {
        return """
                           {
              "id": "https://localhost/47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "identifier": "47ee1fe2-fbe7-4e13-bb71-c78c4f31e7dd",
              "createdDate": "2003-03-27T20:59:02.725Z",
              "modifiedDate": "2010-03-12T11:22:33.135Z",
              "name": "V1cyvCG8ChtM",
              "displayName": "9knaaGRSFr9gM0j",
              "shortName": "nmkPoRSQzc7vURa",
              "archiveName": "BYgxqwqfBjD17KgosJ",
              "cname": "LKjlM7dzZ3ZHw1hGu",
              "institutionDns": "2NwOzIEM2tTv",
              "feideOrganizationDomain": "b58v2c4tNmjwbC2",
              "cristinId": "https://www.example.com/E7nWFINRiV",
              "customerOf": "nva.unit.no",
              "vocabularies": [
                {
                  "type": "Vocabulary",
                  "name": "6q02lavfZkad",
                  "id": "https://www.example.com/3vWcHKglac",
                  "status": "Default"
                }
              ],
              "rorId": "https://www.example.com/9CCuwMlwrHA5",
              "serviceCenter": {
                "uri": "https://www.example.com/vplpKxc5Iz8I",
                "text": "ragknjfsngkj"
              },
              "nviInstitution": false,
              "rboInstitution": false,
              "allowFileUploadForTypes": [
                "ReportWorkingPaper"
              ],
              "type": "Customer"
            }
            """;
    }
}
