package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.interfaces.DoiAgent;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

public class SecretManagerDoiAgentDao implements DoiAgent {

    private URI customerId;
    @JsonProperty("customerDoiPrefix")
    private String prefix;
    @JsonProperty("dataCiteMdsClientUrl")
    private String url;
    @JsonProperty("dataCiteMdsClientUsername")
    private String username;
    @JsonProperty("dataCiteMdsClientPassword")
    private String password;

    @SuppressWarnings("unused")
    public SecretManagerDoiAgentDao() {
    }

    public SecretManagerDoiAgentDao(DoiAgentDto doiAgentDto) {
        this.customerId = agentIdToCustomerId(doiAgentDto.getId());
        this.prefix = doiAgentDto.getPrefix();
        this.url = doiAgentDto.getUrl();
        this.username = doiAgentDto.getUsername();
        this.password = doiAgentDto.getPassword();
    }

    private URI agentIdToCustomerId(URI agentId) {
        return UriWrapper.fromUri(agentId)
            .getParent().orElseThrow().getUri();
    }

    public static SecretManagerDoiAgentDao fromJson(String json) throws BadRequestException {
        return attempt(() -> JsonConfig.readValue(json, SecretManagerDoiAgentDao.class)).orElseThrow(
            fail -> new BadRequestException("Could not parse input:" + json, fail.getException()));
    }

    public SecretManagerDoiAgentDao merge(SecretManagerDoiAgentDao agentDao) {
        merge(agentDao.toDoiAgentDto());
        return this;
    }

    public DoiAgentDto toDoiAgentDto() {
        return new DoiAgentDto(this)
            .addPassword(getPassword())
            .addId(customerIdToAgentId(customerId));
    }

    private URI customerIdToAgentId(URI customerId) {
        return UriWrapper.fromUri(customerId).addChild(DOI_AGENT).getUri();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void merge(DoiAgentDto doiAgent) {
        if (isNull(customerId)) {
            setCustomerId(agentIdToCustomerId(doiAgent.getId()));
        }
        if (nonNull(doiAgent.getPrefix())) {
            setPrefix(doiAgent.getPrefix());
        }
        if (nonNull(doiAgent.getUrl())) {
            setUrl(doiAgent.getUrl());
        }
        if (nonNull(doiAgent.getPassword())) {
            setPassword(doiAgent.getPassword());
        }
        setUsername(doiAgent.getUsername());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getCustomerId(), getPrefix(), getUrl(), getUsername(), getPassword());
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

    /**
     * Boilerplate code ahead.
     */

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

    @Override
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

    @Override
    @JacocoGenerated
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }

}
