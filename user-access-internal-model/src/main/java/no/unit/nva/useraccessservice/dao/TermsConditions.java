package no.unit.nva.useraccessservice.dao;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.useraccessservice.interfaces.DataAccessClass;
import no.unit.nva.useraccessservice.interfaces.DataAccessLayer;
import no.unit.nva.useraccessservice.interfaces.DataAccessService;
import nva.commons.apigateway.exceptions.NotFoundException;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.net.URI;
import java.time.Instant;

import static java.util.Objects.isNull;

@DynamoDbImmutable(builder = TermsConditions.Builder.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
public record TermsConditions(
        @DynamoDbPartitionKey URI id,
        @DynamoDbSortKey String type,
        Instant created,
        URI owner,
        Instant modified,
        URI modifiedBy,
        URI termsConditionsUri
) implements DataAccessLayer<TermsConditions>, DataAccessClass<TermsConditions> {

    @DynamoDbIgnore
    @Override
    public TermsConditions upsert(DataAccessService<TermsConditions> service) throws NotFoundException {
        service.persist(this);
        return fetch(service);
    }

    @DynamoDbIgnore
    @Override
    public TermsConditions fetch(DataAccessService<TermsConditions> service) throws NotFoundException {
        return service.fetch(this);
    }

    @DynamoDbIgnore
    @Override
    public TermsConditions merge(TermsConditions item) {
        return TermsConditions.builder()
                .id(id())
                .type(type())
                .created(created())
                .owner(owner())     //in a persist operation, the owner should not change
                .modified(Instant.now())
                .modifiedBy(item.modifiedBy())
                .termsConditionsUri(item.termsConditionsUri())
                .build();
    }

    public static TermsConditions.Builder builder() {
        return new TermsConditions.Builder();
    }

    public static class Builder {

        private URI id;
        private String type;
        private Instant createdInstant;
        private Instant modifiedInstant;
        private URI modifiedById;
        private URI termsUri;
        private URI owner;

        /**
         * Set the id of the TermsConditions.
         *
         * @param withId the id of the TermsConditions.
         * @return a builder with the id set.
         */
        public Builder id(URI withId) {
            this.id = withId;
            return this;
        }

        /**
         * Set the type of the TermsConditions.
         *
         * @param withType the type of the TermsConditions.
         * @return a builder with the type set.
         */
        public Builder type(String withType) {
            this.type = withType;
            return this;
        }

        /**
         * Set the creation time of the TermsConditions.
         *
         * @param created the creation time of the TermsConditions.
         * @return a builder with the creation time set.
         */
        public Builder created(Instant created) {
            this.createdInstant = created;
            return this;
        }

        /**
         * Set the owner of the TermsConditions.
         *
         * @param currentOwner the owner of the TermsConditions.
         * @return a builder with the owner set.
         */
        public Builder owner(URI currentOwner) {
            this.owner = currentOwner;
            return this;
        }

        /**
         * Set the modification time of the TermsConditions.
         *
         * @param modified the modification time of the TermsConditions.
         * @return a builder with the modification time set.
         */
        public Builder modified(Instant modified) {
            this.modifiedInstant = modified;
            return this;
        }

        /**
         * Set the user that modified the TermsConditions.
         *
         * @param modifiedBy the user that modified the TermsConditions.
         * @return a builder with the user that modified the TermsConditions set.
         */
        public Builder modifiedBy(URI modifiedBy) {
            this.modifiedById = modifiedBy;
            return this;
        }

        /**
         * Set the URI of the TermsConditions.
         *
         * @param termsConditionsUri the URI of the TermsConditions.
         * @return a builder with the URI set.
         */
        public Builder termsConditionsUri(URI termsConditionsUri) {
            this.termsUri = termsConditionsUri;
            return this;
        }

        /**
         * Build the TermsConditions.
         *
         * @return a TermsConditions object.
         */
        public TermsConditions build() {
            if (isNull(type)) {
                type("TermsConditions");
            }
            if (isNull(createdInstant)) {
                created(Instant.now());
                modified(createdInstant);
            }
            if(isNull(owner)) {
                owner = modifiedById;
            }

            return new TermsConditions(id, type, createdInstant, owner,modifiedInstant, modifiedById, termsUri);
        }
    }

}

