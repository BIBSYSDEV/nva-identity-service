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

public class SecretManagerDoiAgentDao implements DoiAgent {

    private URI customerId;
    @JsonProperty("customerDoiPrefix")
    private String prefix;
    @JsonProperty("dataCiteMdsClientUrl")
    private String url;
    @JsonProperty("dataCiteMdsClientUsername")
    private String username;
    @JsonProperty("dataCiteMdsClientPassword")
    private  String password;

    public SecretManagerDoiAgentDao() {
    }

    public SecretManagerDoiAgentDao(URI customerId, DoiAgentDto doiAgentDto) {
        setCustomerId(customerId);
        setPrefix(doiAgentDto.getPrefix());
        setUrl(doiAgentDto.getUrl());
        setUsername(doiAgentDto.getUsername());
        setPassword(doiAgentDto.getPassword());
    }


    private SecretManagerDoiAgentDao(Builder builder) {
        setCustomerId(builder.customerId);
        setPrefix(builder.prefix);
        setUrl(builder.url);
        setUsername(builder.username);
        setPassword(builder.password);
    }

    public static SecretManagerDoiAgentDao fromJson(String json) throws BadRequestException {
        return attempt(() -> JsonConfig.readValue(json, SecretManagerDoiAgentDao.class)).orElseThrow(
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
    public String getUsername() {
        return username;
    }

    public void setUsername(String name) {
        this.username = name;
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
        if (!(o instanceof SecretManagerDoiAgentDao)) {
            return false;
        }
        SecretManagerDoiAgentDao that = (SecretManagerDoiAgentDao) o;
        return Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getPrefix(), that.getPrefix())
               && Objects.equals(getUrl(), that.getUrl())
               && Objects.equals(getUsername(), that.getUsername())
               && Objects.equals(getPassword(), that.getPassword());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getCustomerId(), getPrefix(), getUrl(), getUsername(), getPassword());
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
        private String username;
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

        public Builder withUsername(String name) {
            this.username = name;
            return this;
        }

        public Builder withPassword(String secret) {
            this.password = secret;
            return this;
        }

        public SecretManagerDoiAgentDao build() {
            return new SecretManagerDoiAgentDao(this);
        }
    }
}
