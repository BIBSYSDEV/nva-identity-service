package no.unit.nva.customer.model;

import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

@DynamoDbBean
public class ChannelClaimDao implements JsonSerializable {

    private URI channel;
    private List<PublicationInstanceTypes> scope;
    private List<ChannelConstraint> constraints;

    public ChannelClaimDao() {
        this.scope = emptyList();
        this.constraints = emptyList();
    }

    public ChannelClaimDao(URI channel, List<PublicationInstanceTypes> scope, List<ChannelConstraint> constraints) {
        this.channel = channel;
        this.scope = scope;
        this.constraints = constraints;
    }

    public static ChannelClaimDao fromDto(ChannelClaimDto dto) {
        return new ChannelClaimDao(dto.getChannel(), dto.getScope(), dto.getConstraints());
    }

    public ChannelClaimDto toDto() {
        return new ChannelClaimDto(getChannel(), getScope(), getConstraints());
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChannelClaimDao that = (ChannelClaimDao) o;
        return Objects.equals(getChannel(), that.getChannel())
                && Objects.equals(getScope(), that.getScope())
                && Objects.equals(getConstraints(), that.getConstraints());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getChannel(), getScope(), getConstraints());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }
}
