package no.unit.nva.cognito.cristin.org;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

public class CristinOrgResponse {

    @JsonProperty("id")
    private URI orgId;
    @JsonProperty("partOf")
    private List<CristinOrgResponse> partOf;

    public static CristinOrgResponse fromJson(String body) {
        return attempt(() -> JsonConfig.beanFrom(CristinOrgResponse.class, body)).orElseThrow();
    }

    public static CristinOrgResponse create(URI orgId) {
        var response = new CristinOrgResponse();
        response.setOrgId(orgId.toString());
        return response;
    }

    public static CristinOrgResponse create(URI orgId, URI... parentIds) {
        var result = CristinOrgResponse.create(orgId);
        Queue<CristinOrgResponse> parentsQueue = addAllParentIdsInQueue(parentIds);
        var currentOrg = result;
        while (!parentsQueue.isEmpty()) {
            var oneLevelUp = parentsQueue.poll();
            currentOrg.setPartOf(List.of(oneLevelUp));
            currentOrg = oneLevelUp;
        }
        return result;
    }

    @JacocoGenerated
    public List<CristinOrgResponse> getPartOf() {
        return nonNull(partOf) ? partOf : Collections.emptyList();
    }

    @JacocoGenerated
    public void setPartOf(List<CristinOrgResponse> partOf) {
        this.partOf = nonNull(partOf) ? partOf : null;
    }

    public URI extractTopOrgUri() {
        if (isNull(partOf) || partOf.isEmpty()) {
            return orgId;
        } else {
            return partOf.get(0).extractTopOrgUri();
        }
    }

    @JacocoGenerated
    public String getOrgId() {
        return nonNull(orgId) ? orgId.toString() : null;
    }

    @JacocoGenerated
    public void setOrgId(URI orgId) {
        this.orgId = orgId;
    }

    @JacocoGenerated
    public void setOrgId(String orgId) {
        this.orgId = nonNull(orgId) ? URI.create(orgId) : null;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return attempt(() -> JsonConfig.asString(this)).orElseThrow();
    }

    private static ArrayDeque<CristinOrgResponse> addAllParentIdsInQueue(URI... partOfOrgIds) {
        var parentsQueue = new ArrayDeque<CristinOrgResponse>();
        if (nonNull(partOfOrgIds) && partOfOrgIds.length > 0) {
            var parents = Optional.of(partOfOrgIds).stream()
                .flatMap(Arrays::stream)
                .map(CristinOrgResponse::create)
                .collect(Collectors.toList());
            parentsQueue.addAll(parents);
        }
        return parentsQueue;
    }
}
