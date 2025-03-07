package no.unit.nva.customer.model;

import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.*;

import static java.util.Objects.nonNull;

public class ChannelClaimDto {

    private URI channel;
    private List<PublicationInstanceTypes> scope;
    private List<ChannelConstraint> constraints;

    public ChannelClaimDto() {
        this.scope = Collections.emptyList();
        this.constraints = Collections.emptyList();
    }

    public static Builder builder() {
        return new ChannelClaimDto.Builder();
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

    public static final class Builder {
        private final ChannelClaimDto channelClaimDto;

        private Builder() {
            channelClaimDto = new ChannelClaimDto();
        }

        public Builder withChannel(URI channel) {
            channelClaimDto.setChannel(channel);
            return this;
        }

        public Builder withScope(Collection<PublicationInstanceTypes> scope) {
            if (nonNull(scope)) {
                channelClaimDto.setScope(new ArrayList<>(scope));
            }
            return this;
        }

        public Builder withConstraints(Collection<ChannelConstraint> constraints) {
            if (nonNull(constraints)) {
                channelClaimDto.setConstraints(new ArrayList<>(constraints));
            }
            return this;
        }

        public ChannelClaimDto build() {
            return channelClaimDto;
        }
    }
}
