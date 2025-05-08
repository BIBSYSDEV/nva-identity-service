package no.unit.nva.customer.events.model;

import com.fasterxml.jackson.annotation.JsonValue;

public record ResourceUpdateEvent<T extends IdentifiedResource>(Action action, String resourceType, T data) {

    public enum Action {
        ADDED("Added"), REMOVED("Removed"), UPDATED("Updated");
        private final String value;

        @JsonValue
        public String getValue() {
            return value;
        }

        Action(String value) {
            this.value = value;
        }
    }
}
