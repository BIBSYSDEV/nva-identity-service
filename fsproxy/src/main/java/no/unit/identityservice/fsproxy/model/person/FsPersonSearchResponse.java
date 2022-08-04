package no.unit.identityservice.fsproxy.model.person;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

public class FsPersonSearchResponse implements JsonSerializable {
    
    @JsonProperty("items")
    private final List<FsIdSearchResult> searchResults;
    
    @JsonCreator
    public FsPersonSearchResponse(@JsonProperty("items") List<FsIdSearchResult> searchResult) {
        this.searchResults = nonNull(searchResult) ? searchResult : Collections.emptyList();
    }
    
    public List<FsIdSearchResult> getSearchResults() {
        return searchResults;
    }
    
    @Override
    public String toString() {
        return toJsonString();
    }
}
