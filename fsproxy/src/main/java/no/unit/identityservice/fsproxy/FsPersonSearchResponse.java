package no.unit.identityservice.fsproxy;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class FsPersonSearchResponse {
    @JsonProperty("items")
    private List<FsIdSearchResult> searchResults;

    public List<FsIdSearchResult> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(List<FsIdSearchResult> searchResults) {
        this.searchResults = searchResults;
    }
}
