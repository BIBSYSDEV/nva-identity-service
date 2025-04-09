package no.unit.nva.useraccessservice.dao;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.database.interfaces.DataAccessClass;
import no.unit.nva.database.interfaces.DataAccessLayer;
import no.unit.nva.database.interfaces.DataAccessService;
import nva.commons.apigateway.exceptions.NotFoundException;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.net.URI;
import java.time.Instant;

import static java.util.Objects.isNull;

@SuppressWarnings("PMD.ShortMethodName")
@DynamoDbImmutable(builder = TermsConditions.Builder.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
public record TermsConditions(
    @DynamoDbPartitionKey URI id,
    @DynamoDbSortKey String type,
    Instant created,
    String owner,
    Instant modified,
    String modifiedBy,
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
        return builder()
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

        private URI withId;
        private String withType;
        private Instant createdInstant;
        private Instant modifiedInstant;
        private String modifiedById;
        private URI termsUri;
        private String withOwner;

        /**
         * Set the id of the TermsConditions.
         *
         * @param withId the id of the TermsConditions.
         * @return a builder with the id set.
         */
        public Builder id(URI withId) {
            this.withId = withId;
            return this;
        }

        /**
         * Set the owner of the TermsConditions.
         *
         * @param currentOwner the owner of the TermsConditions.
         * @return a builder with the owner set.
         */
        public Builder owner(String currentOwner) {
            this.withOwner = currentOwner;
            return this;
        }

        /**
         * Set the user that modified the TermsConditions.
         *
         * @param modifiedBy the user that modified the TermsConditions.
         * @return a builder with the user that modified the TermsConditions set.
         */
        public Builder modifiedBy(String modifiedBy) {
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
            if (isNull(withType)) {
                type("TermsConditions");
            }
            if (isNull(createdInstant)) {
                created(Instant.now());
                modified(createdInstant);
            }
            if (isNull(withOwner)) {
                withOwner = modifiedById;
            }

            return new TermsConditions(withId, withType, createdInstant, withOwner, modifiedInstant, modifiedById,
                termsUri);
        }

        /**
         * Set the type of the TermsConditions.
         *
         * @param withType the type of the TermsConditions.
         * @return a builder with the type set.
         */
        public Builder type(String withType) {
            this.withType = withType;
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
         * Set the modification time of the TermsConditions.
         *
         * @param modified the modification time of the TermsConditions.
         * @return a builder with the modification time set.
         */
        public Builder modified(Instant modified) {
            this.modifiedInstant = modified;
            return this;
        }
    }

}

