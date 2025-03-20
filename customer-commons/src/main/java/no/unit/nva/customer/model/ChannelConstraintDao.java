package no.unit.nva.customer.model;

import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static no.unit.nva.customer.model.ChannelConstraintPolicy.EVERYONE;
import static no.unit.nva.customer.model.ChannelConstraintPolicy.OWNER_ONLY;

@DynamoDbBean
public class ChannelConstraintDao implements JsonSerializable {
    private ChannelConstraintPolicy publishesMetadataPolicy;
    private ChannelConstraintPolicy editsMetadataPolicy;
    private List<PublicationInstanceTypes> scope;

    public ChannelConstraintDao() {
        this.publishesMetadataPolicy = EVERYONE;
        this.editsMetadataPolicy = OWNER_ONLY;
        this.scope = emptyList();
    }

    public ChannelConstraintDao(
            ChannelConstraintPolicy publishesMetadataPolicy,
            ChannelConstraintPolicy editsMetadataPolicy,
            List<PublicationInstanceTypes> scope) {
        this.publishesMetadataPolicy = publishesMetadataPolicy;
        this.editsMetadataPolicy = editsMetadataPolicy;
        this.scope = scope;
    }

    public static ChannelConstraintDao fromDto(ChannelConstraintDto dto) {
        return new ChannelConstraintDao(dto.publishesMetadataPolicy(), dto.editsMetadataPolicy(), dto.scope());
    }

    public ChannelConstraintDto toDto() {
        return new ChannelConstraintDto(getPublishesMetadataPolicy(), getEditsMetadataPolicy(), getScope());
    }

    public ChannelConstraintPolicy getPublishesMetadataPolicy() {
        return publishesMetadataPolicy;
    }

    @JacocoGenerated
    public void setPublishesMetadataPolicy(ChannelConstraintPolicy publishesMetadataPolicy) {
        this.publishesMetadataPolicy = publishesMetadataPolicy;
    }

    public ChannelConstraintPolicy getEditsMetadataPolicy() {
        return editsMetadataPolicy;
    }

    @JacocoGenerated
    public void setEditsMetadataPolicy(ChannelConstraintPolicy editsMetadataPolicy) {
        this.editsMetadataPolicy = editsMetadataPolicy;
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
        ChannelConstraintDao that = (ChannelConstraintDao) o;
        return getPublishesMetadataPolicy() == that.getPublishesMetadataPolicy()
                && getEditsMetadataPolicy() == that.getEditsMetadataPolicy()
                && Objects.equals(getScope(), that.getScope());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPublishesMetadataPolicy(), getEditsMetadataPolicy(), getScope());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }
}