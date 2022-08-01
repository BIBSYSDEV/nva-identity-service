package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("PMD")

public class FsEmne {

    @JsonProperty("kode")
    final String code;

    @JsonCreator
    public FsEmne(@JsonProperty("kode") String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
