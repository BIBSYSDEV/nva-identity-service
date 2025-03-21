package no.unit.nva.customer.model;

import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.net.URI;
import java.util.Objects;

@DynamoDbBean
public class ChannelClaimDao implements JsonSerializable {

    private URI channel;
    private ChannelConstraintDao constraint;

    public ChannelClaimDao() {
    }

    public ChannelClaimDao(URI channel, ChannelConstraintDao constraint) {
        this.channel = channel;
        this.constraint = constraint;
    }

    public static ChannelClaimDao fromDto(ChannelClaimDto dto) {
        return new ChannelClaimDao(dto.channel(), ChannelConstraintDao.fromDto(dto.constraint()));
    }

    public ChannelClaimDto toDto() {
        return new ChannelClaimDto(getChannel(), getConstraint().toDto());
    }

    public URI getChannel() {
        return channel;
    }

    @JacocoGenerated
    public void setChannel(URI channel) {
        this.channel = channel;
    }

    public ChannelConstraintDao getConstraint() {
        return constraint;
    }

    @JacocoGenerated
    public void setConstraint(ChannelConstraintDao constraint) {
        this.constraint = constraint;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelClaimDao that)) {
            return false;
        }
        return Objects.equals(getChannel(), that.getChannel()) && Objects.equals(getConstraint(), that.getConstraint());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getChannel(), getConstraint());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }
}
