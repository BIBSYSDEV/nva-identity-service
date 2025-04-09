package no.unit.nva.customer.model.channelclaim;

import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.net.URI;
import java.util.Objects;

@DynamoDbBean
public class ChannelClaimDao implements JsonSerializable {

    private URI channelId;
    private ChannelConstraintDao constraint;

    public ChannelClaimDao() {
    }

    public ChannelClaimDao(URI channel, ChannelConstraintDao constraint) {
        this.channelId = channel;
        this.constraint = constraint;
    }

    public static ChannelClaimDao fromDto(ChannelClaimDto dto) {
        return new ChannelClaimDao(dto.channelId(), ChannelConstraintDao.fromDto(dto.constraint()));
    }

    public ChannelClaimDto toDto() {
        return new ChannelClaimDto(getChannelId(), getConstraint().toDto());
    }

    public URI getChannelId() {
        return channelId;
    }

    @JacocoGenerated
    public void setChannelId(URI channelId) {
        this.channelId = channelId;
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
        return Objects.equals(getChannelId(), that.getChannelId()) && Objects.equals(getConstraint(), that.getConstraint());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getChannelId(), getConstraint());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }
}
