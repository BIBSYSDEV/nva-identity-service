package no.unit.nva.customer.events.model;

public record ResourceUpdateEvent<T extends IdentifiedResource>(Action action, String resourceType, T data) {

    public enum Action {ADDED, REMOVED, UPDATED}
}
