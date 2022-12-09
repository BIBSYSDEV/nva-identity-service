package no.unit.nva.customer.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.interfaces.DoiAgent;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class SecretManagerDoiAgent implements DoiAgent {

    private URI customerId;
    @JsonProperty("customerDoiPrefix")
    private String prefix;
    @JsonProperty("dataCiteMdsClientUrl")
    private String url;
    @JsonProperty("dataCiteMdsClientUsername")
    private String name;
    @JsonProperty("dataCiteMdsClientPassword")
    private  String password;

    public SecretManagerDoiAgent() {
    }

    public SecretManagerDoiAgent(URI customerId,DoiAgentDto doiAgentDto) {
        setCustomerId(customerId);
        setPrefix(doiAgentDto.getPrefix());
        setUrl(doiAgentDto.getUrl());
        setName(doiAgentDto.getName());
        setPassword(doiAgentDto.getPassword());
    }


    private SecretManagerDoiAgent(Builder builder) {
        setCustomerId(builder.customerId);
        setPrefix(builder.prefix);
        setUrl(builder.url);
        setName(builder.name);
        setPassword(builder.password);
    }

    public static SecretManagerDoiAgent fromJson(String json) throws BadRequestException {
        return attempt(() -> JsonConfig.readValue(json, SecretManagerDoiAgent.class)).orElseThrow(
            fail -> new BadRequestException("Could not parse input:" + json, fail.getException()));
    }

    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecretManagerDoiAgent that)) {
            return false;
        }
        return Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getPrefix(), that.getPrefix())
               && Objects.equals(getUrl(), that.getUrl())
               && Objects.equals(getName(), that.getName())
               && Objects.equals(getPassword(), that.getPassword());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getCustomerId(), getPrefix(), getUrl(), getName(), getPassword());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }

    public static final class Builder {

        private URI customerId;
        private String prefix;
        private String url;
        private String name;
        private String password;

        public Builder() {
        }

        public Builder withCustomerId(URI customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPassword(String secret) {
            this.password = secret;
            return this;
        }

        public SecretManagerDoiAgent build() {
            return new SecretManagerDoiAgent(this);
        }
    }
}
