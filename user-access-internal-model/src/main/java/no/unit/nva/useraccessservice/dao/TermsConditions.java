package no.unit.nva.useraccessservice.dao;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.commons.json.JsonSerializable;

import no.unit.nva.useraccessservice.interfaces.DataAccessClass;
import no.unit.nva.useraccessservice.interfaces.DataAccessLayer;
import no.unit.nva.useraccessservice.interfaces.DataAccessService;
import nva.commons.apigateway.exceptions.NotFoundException;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.net.URI;
import java.time.Instant;

import static java.util.Objects.isNull;

@DynamoDbImmutable(builder = TermsConditions.Builder.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = TermsConditions.WITH_TYPE, visible = true)
public record TermsConditions(
        @DynamoDbPartitionKey URI withId,
        @DynamoDbSortKey String withType,
        Instant created,
        Instant modified,
        URI modifiedBy,
        URI termsConditionsUri
) implements JsonSerializable, DataAccessLayer<TermsConditions>, DataAccessClass<TermsConditions> {


    public static final String WITH_TYPE = "withType";

    @Override
    public TermsConditions upsert(DataAccessService<TermsConditions> service) throws NotFoundException {
        service.persist(this);
        return fetch(service);
    }

    @Override
    public TermsConditions fetch(DataAccessService<TermsConditions> service) throws NotFoundException {
        return service.fetch(this);
    }

    @Override
    public TermsConditions merge(TermsConditions item) {
        return TermsConditions.builder()
                .withId(withId())
                .withType(withType())
                .created(created())
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
        private Instant created;
        private Instant modified;
        private URI modifiedBy;
        private URI termsConditionsUri;

        public Builder withId(URI withId) {
            this.withId = withId;
            return this;
        }

        public Builder withType(String withType) {
            this.withType = withType;
            return this;
        }

        public Builder created(Instant created) {
            this.created = created;
            return this;
        }

        public Builder modified(Instant modified) {
            this.modified = modified;
            return this;
        }

        public Builder modifiedBy(URI modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        public Builder termsConditionsUri(URI termsConditionsUri) {
            this.termsConditionsUri = termsConditionsUri;
            return this;
        }


        public TermsConditions build() {
            if (isNull(withType)) {
                withType(this.getClass().getSimpleName());
            }
            if(isNull(modified)) {
                modified(Instant.now());
            }
            if(isNull(created)) {
                created(modified);
            }
            return new TermsConditions(withId, withType, created, modified, modifiedBy, termsConditionsUri);
        }
    }

}

