package no.unit.nva.customer.model;

import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

public class ChannelClaimDto {
    private URI channel;
    private List<PublicationInstanceTypes> scope;
    private List<ChannelConstraint> constraints;

    public ChannelClaimDto() {
        this.scope = emptyList();
        this.constraints = emptyList();
    }

    public ChannelClaimDto(URI channel, List<PublicationInstanceTypes> scope, List<ChannelConstraint> constraints) {
        this.channel = channel;
        this.scope = scope;
        this.constraints = constraints;
    }

    public URI getChannel() {
        return channel;
    }

    public void setChannel(URI channel) {
        this.channel = channel;
    }

    public List<PublicationInstanceTypes> getScope() {
        return scope;
    }

    public void setScope(List<PublicationInstanceTypes> scope) {
        this.scope = scope;
    }

    public List<ChannelConstraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ChannelConstraint> constraints) {
        this.constraints = constraints;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChannelClaimDto that = (ChannelClaimDto) o;
        return Objects.equals(getChannel(), that.getChannel())
                && Objects.equals(getScope(), that.getScope())
                && Objects.equals(getConstraints(), that.getConstraints());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getChannel(), getScope(), getConstraints());
    }
}
