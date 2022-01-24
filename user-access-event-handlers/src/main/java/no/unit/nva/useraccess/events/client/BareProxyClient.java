package no.unit.nva.useraccess.events.client;

import java.net.URI;
import java.util.Optional;

public interface BareProxyClient {

    Optional<SimpleAuthorityResponse> getAuthorityByFeideId(String feideId);

    void deleteAuthorityOrganizationId(String systemControlNumber, URI organizationId);

}
