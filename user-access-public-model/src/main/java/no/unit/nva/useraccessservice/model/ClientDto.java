package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.interfaces.Typed;
import no.unit.nva.useraccessservice.interfaces.WithCopy;
import no.unit.nva.useraccessservice.model.ClientDto.Builder;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

import static nva.commons.core.attempt.Try.attempt;

public class ClientDto implements WithCopy<Builder>, Typed {

    public static final String TYPE = "Client";
    public static final String CLIENT_ID_FIELD = "client";
    public static final String CUSTOMER_FIELD = "customerUri";
    public static final String CRISTIN_ORG_FIELD = "cristinOrgUri";
    public static final String ACTING_USER_FIELD = "actingUser";
    public static final String AT = "@";

    @JsonProperty(CLIENT_ID_FIELD)
    private String clientId;

    @JsonProperty(CUSTOMER_FIELD)
    private URI customer;

    @JsonProperty(CRISTIN_ORG_FIELD)
    private URI cristinOrgUri;
    @JsonProperty(ACTING_USER_FIELD)
    private String actingUser;

    public ClientDto() {

    }

    /**
     * returns a new builder.
     *
     * @return a new {@link ClientDto.Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static ClientDto fromJson(String input) throws BadRequestException {
        return attempt(() -> JsonConfig.readValue(input, ClientDto.class))
                .orElseThrow(fail -> new BadRequestException("Could not read Client:" + input, fail.getException()));
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public URI getCustomer() {
        return customer;
    }

    public void setCustomer(URI customer) {
        this.customer = customer;
    }

    public URI getCristinOrgUri() {
        return cristinOrgUri;
    }

    public void setCristinOrgUri(URI cristinOrgUri) {
        this.cristinOrgUri = cristinOrgUri;
    }

    public String getActingUser() {
        return actingUser;
    }

    public void setActingUser(String actingUser) {
        this.actingUser = actingUser;
    }

    @Override
    @JsonProperty(Typed.TYPE_FIELD)
    public String getType() {
        return ClientDto.TYPE;
    }

    @Override
    public void setType(String type) throws BadRequestException {
        Typed.super.setType(type);
    }

    @Override
    public ClientDto.Builder copy() {
        return new Builder()
                .withClientId(getClientId())
                .withCustomer(getCustomer())
                .withCristinOrgUri(getCristinOrgUri())
                .withActingUser(getActingUser());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getClientId(), getCustomer());
    }


    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientDto)) {
            return false;
        }
        ClientDto clientDto = (ClientDto) o;
        return Objects.equals(getClientId(), clientDto.getClientId())
                && Objects.equals(getCustomer(), clientDto.getCustomer())
                && Objects.equals(getCristinOrgUri(), clientDto.getCristinOrgUri())
                && Objects.equals(getActingUser(), clientDto.getActingUser());
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }

    public static final class Builder {

        private final ClientDto clientDto;

        private Builder() {
            clientDto = new ClientDto();
        }

        public Builder withClientId(String clientId) {
            clientDto.setClientId(clientId);
            return this;
        }

        public Builder withCustomer(URI customer) {
            clientDto.setCustomer(customer);
            return this;
        }

        public Builder withCristinOrgUri(URI cristinOrgUri) {
            clientDto.setCristinOrgUri(cristinOrgUri);
            return this;
        }

        public Builder withActingUser(String actingUser) {
            clientDto.setActingUser(actingUser);
            return this;
        }

        public ClientDto build() {
            return clientDto;
        }
    }
}
