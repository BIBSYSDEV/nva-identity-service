package no.unit.identityservice.fsproxy.model.person;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class FsPersonSearchResponse implements JsonSerializable {

    @JsonProperty("items")
    private final List<FsIdSearchResult> searchResults;

    @JsonCreator
    public FsPersonSearchResponse(@JsonProperty("items") List<FsIdSearchResult> searchResult) {
        this.searchResults = nonNull(searchResult) ? searchResult : Collections.emptyList();
    }

    public static FsPersonSearchResponse fromJson(String responseBody) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(responseBody, FsPersonSearchResponse.class);
    }

    public Optional<FsIdSearchResult> getSearchResult() {
        if (searchResults.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(searchResults.get(0));
    }

    @Override
    public String toString() {
        return toJsonString();
    }
}
