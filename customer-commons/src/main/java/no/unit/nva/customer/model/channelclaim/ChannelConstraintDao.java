package no.unit.nva.customer.model.channelclaim;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy.EVERYONE;
import static no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy.OWNER_ONLY;

@DynamoDbBean
public class ChannelConstraintDao implements JsonSerializable {
    private ChannelConstraintPolicy publishingPolicy;
    private ChannelConstraintPolicy editingPolicy;
    private List<PublicationInstanceTypes> scope;

    public ChannelConstraintDao() {
        this.publishingPolicy = EVERYONE;
        this.editingPolicy = OWNER_ONLY;
        this.scope = emptyList();
    }

    public ChannelConstraintDao(
            ChannelConstraintPolicy publishingPolicy,
            ChannelConstraintPolicy editingPolicy,
            List<PublicationInstanceTypes> scope) {
        this.publishingPolicy = publishingPolicy;
        this.editingPolicy = editingPolicy;
        this.scope = scope;
    }

    public static ChannelConstraintDao fromDto(ChannelConstraintDto dto) {
        return new ChannelConstraintDao(dto.publishingPolicy(), dto.editingPolicy(), dto.scope());
    }

    public ChannelConstraintDto toDto() {
        return new ChannelConstraintDto(getPublishingPolicy(), getEditingPolicy(), getScope());
    }

    public ChannelConstraintPolicy getPublishingPolicy() {
        return publishingPolicy;
    }

    @JacocoGenerated
    public void setPublishingPolicy(ChannelConstraintPolicy publishingPolicy) {
        this.publishingPolicy = publishingPolicy;
    }

    public ChannelConstraintPolicy getEditingPolicy() {
        return editingPolicy;
    }

    @JacocoGenerated
    public void setEditingPolicy(ChannelConstraintPolicy editingPolicy) {
        this.editingPolicy = editingPolicy;
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
        if (!(o instanceof ChannelConstraintDao that)) {
            return false;
        }
        return Objects.equals(getPublishingPolicy(), that.getPublishingPolicy()) &&
                Objects.equals(getEditingPolicy(), that.getEditingPolicy()) &&
                Objects.equals(getScope(), that.getScope());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPublishingPolicy(), getEditingPolicy(), getScope());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }
}