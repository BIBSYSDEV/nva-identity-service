package no.unit.identityservice.fsproxy.model.Person;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FsPersonSearchResponse {

    @JsonProperty("items")
    private final List<FsIdSearchResult> searchResults;

    @JsonCreator
    public FsPersonSearchResponse(@JsonProperty("items") List<FsIdSearchResult> searchResult) {
        this.searchResults = searchResult;
    }

    public List<FsIdSearchResult> getSearchResults() {
        return searchResults;
    }
}
