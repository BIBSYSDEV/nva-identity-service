package no.unit.nva.useraccessservice.dao;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.database.interfaces.DataAccessClass;
import no.unit.nva.database.interfaces.DataAccessLayer;
import no.unit.nva.database.interfaces.DataAccessService;
import nva.commons.apigateway.exceptions.NotFoundException;
import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.net.URI;
import java.time.Instant;

import static java.util.Objects.isNull;

@SuppressWarnings("PMD.ShortMethodName")
@DynamoDbImmutable(builder = Terms.Builder.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
public record Terms(
  @DynamoDbPartitionKey String id,
  @DynamoDbSortKey String type,
  Instant created,
  String createdBy,
  Instant modified,
  String modifiedBy,
  URI termsConditionsUri,
  Instant validFrom

) implements DataAccessLayer<Terms>, DataAccessClass<Terms>, Comparable<Terms> {

    @DynamoDbIgnore
    @Override
    public Terms upsert(DataAccessService<Terms> service) throws NotFoundException {
        service.persist(this);
        return fetch(service);
    }

    @DynamoDbIgnore
    @Override
    public Terms fetch(DataAccessService<Terms> service) throws NotFoundException {
        return service.fetch(this);
    }


    @DynamoDbIgnore
    @Override
    public Terms merge(Terms item) {
        return Terms.builder()
            .id(id())
            .type(type())
            .created(created())
            .createdBy(createdBy())     //in a persist operation, the owner should not change
            .modified(Instant.now())
            .modifiedBy(item.modifiedBy())
            .termsConditionsUri(item.termsConditionsUri())
            .validFrom(item.validFrom())
            .build();
    }

    @Override
    @DynamoDbIgnore
    public int compareTo(@NotNull Terms other) {
        int result;
        if (isNull(this.validFrom) || isNull(other.validFrom)) {
            result = 0;
        } else if (this.validFrom.compareTo(Instant.now()) > 0) {
            result = -1;
        } else {
            result = this.validFrom.compareTo(other.validFrom);
        }
        return result;
    }

    public static Terms.Builder builder() {
        return new Terms.Builder();
    }

    public static class Builder {

        private String withId;
        private String withType;
        private Instant createdInstant;
        private Instant modifiedInstant;
        private String modifiedById;
        private URI termsUri;
        private String withOwner;
        private Instant validFrom;

        /**
         * Set the id of the TermsConditions.
         *
         * @param withId the id of the TermsConditions.
         * @return a builder with the id set.
         */
        public Builder id(String withId) {
            this.withId = withId;
            return this;
        }

        /**
         * Set the owner of the TermsConditions.
         *
         * @param currentOwner the owner of the TermsConditions.
         * @return a builder with the owner set.
         */
        public Builder createdBy(String currentOwner) {
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
        public Terms build() {
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

            return new Terms(withId, withType, createdInstant, withOwner, modifiedInstant, modifiedById,
                termsUri, validFrom);
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

        public Builder validFrom(Instant instant) {
            this.validFrom = instant;
            return this;
        }
    }

}

