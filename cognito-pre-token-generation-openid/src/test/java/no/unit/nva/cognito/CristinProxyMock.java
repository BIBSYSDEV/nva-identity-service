package no.unit.nva.cognito;

import com.github.tomakehurst.wiremock.WireMockServer;

public class CristinProxyMock {

    private final WireMockServer httpServer;

    public CristinProxyMock(WireMockServer httpServer) {
        this.httpServer = httpServer;
    }


}
