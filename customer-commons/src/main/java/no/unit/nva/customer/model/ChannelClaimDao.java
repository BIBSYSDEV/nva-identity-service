package no.unit.nva.customer.model;

import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.nonNull;

@DynamoDbBean
public class ChannelClaimDao implements JsonSerializable {

    private URI channel;
    private List<PublicationInstanceTypes> scope;
    private List<ChannelConstraint> constraints;

    @JacocoGenerated
    public ChannelClaimDao() {
        super();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ChannelClaimDao fromDto(ChannelClaimDto dto) {
        return new ChannelClaimDao.Builder()
                .withChannel(dto.getChannel())
                .withScope(dto.getScope())
                .withConstraints(dto.getConstraints())
                .build();
    }

    public ChannelClaimDto toDto() {
        return ChannelClaimDto.builder()
                .withChannel(getChannel())
                .withScope(getScope())
                .withConstraints(getConstraints())
                .build();
    }

    public URI getChannel() {
        return channel;
    }

    @JacocoGenerated
    public void setChannel(URI channel) {
        this.channel = channel;
    }

    public List<ChannelConstraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ChannelConstraint> constraints) {
        this.constraints = constraints;
    }

    public List<PublicationInstanceTypes> getScope() {
        return scope;
    }

    @JacocoGenerated
    public void setScope(List<PublicationInstanceTypes> scope) {
        this.scope = scope;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChannelClaimDao that = (ChannelClaimDao) o;
        return Objects.equals(channel, that.channel) && Objects.equals(scope, that.scope) && Objects.equals(constraints, that.constraints);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(channel, scope, constraints);
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }

    public static final class Builder {

        private final ChannelClaimDao channelClaimDao;

        private Builder() {
            channelClaimDao = new ChannelClaimDao();
        }

        public Builder withChannel(URI channel) {
            channelClaimDao.setChannel(channel);
            return this;
        }

        public Builder withScope(Collection<PublicationInstanceTypes> scope) {
            if (nonNull(scope)) {
                channelClaimDao.setScope(new ArrayList<>(scope));
            }
            return this;
        }

        public Builder withConstraints(Collection<ChannelConstraint> constraints) {
            if (nonNull(constraints)) {
                channelClaimDao.setConstraints(new ArrayList<>(constraints));
            }
            return this;
        }

        public ChannelClaimDao build() {
            return channelClaimDao;
        }
    }
}
