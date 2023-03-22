package no.unit.nva.useraccessservice.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.interfaces.Typed;
import no.unit.nva.useraccessservice.interfaces.WithCopy;
import no.unit.nva.useraccessservice.model.ClientDto.Builder;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class ClientDto implements WithCopy<Builder>, Typed {

    public static final String TYPE = "Client";
    public static final String CLIENT_ID_FIELD = "client";
    public static final String CUSTOMER_FIELD = "customer";
    public static final String CRISTIN_FIELD = "ccristin";
    public static final String OWNER_FIELD = "owner";
    public static final String AT = "@";

    @JsonProperty(CLIENT_ID_FIELD)
    private String clientId;

    @JsonProperty(CUSTOMER_FIELD)
    private URI customer;

    @JsonProperty(CRISTIN_FIELD)
    private URI cristin;
    @JsonProperty(OWNER_FIELD)
    private String owner;

    public ClientDto() {

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

    public URI getCristin() {
        return cristin;
    }

    public String getOwner() {
        return owner;
    }

    public void setCustomer(URI customer) {
        this.customer = customer;
    }

    public void setCristin(URI cristin) {
        this.cristin = cristin;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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
                   .withCristin(getCristin())
                   .withOwner(getOwner());
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
               && Objects.equals(getCristin(), clientDto.getCristin())
               && Objects.equals(getOwner(), clientDto.getOwner());
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

        public Builder withCristin(URI cristin) {
            clientDto.setCristin(cristin);
            return this;
        }

        public Builder withOwner(String owner) {
            clientDto.setOwner(owner);
            return this;
        }
        
        public ClientDto build() {
            return clientDto;
        }
    }
}
