package no.unit.nva.useraccessmanagement.internals;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.List;
import java.util.Map;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.JacocoGenerated;

public class UserScanResult {

    private final List<UserDto> retrievedUsers;
    private final Map<String, AttributeValue> startMarkerForNextScan;
    private final boolean moreEntriesExist;

    public UserScanResult(List<UserDto> retrievedUsers,
                          Map<String, AttributeValue> startMarkerForNextScan, boolean thereAreMoreEntries) {
        this.retrievedUsers = retrievedUsers;
        this.startMarkerForNextScan = startMarkerForNextScan;
        this.moreEntriesExist = thereAreMoreEntries;
    }

    @JacocoGenerated
    public boolean thereAreMoreEntries() {
        return moreEntriesExist;
    }

    @JacocoGenerated
    public List<UserDto> getRetrievedUsers() {
        return retrievedUsers;
    }

    @JacocoGenerated
    public Map<String, AttributeValue> getStartMarkerForNextScan() {
        return startMarkerForNextScan;
    }
}