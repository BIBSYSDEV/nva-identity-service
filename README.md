# nva-identity-service

Identity management and login service for users, roles and customer institutions

## cognito-pre-token-generation

Lambda function used by Cognito during authentication. Augments the access token with extra user
information.

## Output

### Resource update events

#### Channel claims

The stack emits EventBridge events representing changes regarding channel claims using detailType
`nva.resourceupdate.channelclaim`.
Supported actions are:

* Added
* Updated
* Removed

The events look like this:

```json
{
  "source": "NVA.IdentityService.Customer",
  "detail-type": "nva.resourceupdate.channelclaim",
  "detail": {
    "action": "Updated",
    "resourceType": "ChannelClaim",
    "data": {
      "id": "https://api.sandbox.nva.aws.unit.no/customer/channel-claim/1864A370-80CA-4BE5-9CB7-40B0CCEF23CA",
      "channelId": "https://api.sandbox.nva.aws.unit.no/publication-channels-v2/serial-publication/1864A370-80CA-4BE5-9CB7-40B0CCEF23CA",
      "customerId": "https://api.sandbox.nva.aws.unit.no/customer/14d49ab7-4d1d-464d-b732-54b5c46ce6cc",
      "organizationId": "https://api.sandbox.nva.aws.unit.no/cristin/organization/20754.0.0.0",
      "constraint": {
        "scope": [
          "DegreeBachelor",
          "DegreeMaster",
          "DegreePhd",
          "DegreeLicentiate",
          "ArtisticDegreePhd",
          "OtherStudentWork"
        ],
        "publishingPolicy": "Everyone",
        "editingPolicy": "OwnerOnly"
      }
    }
  }
}
```