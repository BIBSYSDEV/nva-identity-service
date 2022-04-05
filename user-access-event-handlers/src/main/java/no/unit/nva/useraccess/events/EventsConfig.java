package no.unit.nva.useraccess.events;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

public final class EventsConfig {

    public static final String IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC = "IdentityService.IdentityEntry.ScanAndUpdate";
    public static final String SCAN_REQUEST_EVENTS_DETAIL_TYPE = "topicInDetailType";
    public static final Environment ENVIRONMENT = new Environment();
    public static final String EVENT_BUS = ENVIRONMENT.readEnv("EVENT_BUS");
    public static final String AWS_REGION = ENVIRONMENT.readEnv("AWS_REGION");

    public static final EventBridgeClient EVENTS_CLIENT = defaultEventBridgeClient();

    private EventsConfig() {

    }

    @JacocoGenerated
    private static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder()
            .region(Region.of(AWS_REGION))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create()).build();
    }
}
