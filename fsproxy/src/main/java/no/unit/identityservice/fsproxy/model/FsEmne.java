package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class FsEmne {

    @JsonProperty("kode")
    private final String code;

    @JsonCreator
    public FsEmne(@JsonProperty("kode") String code) {
        this.code = code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FsEmne fsEmne = (FsEmne) o;
        return code.equals(fsEmne.code);
    }

    public String getCode() {
        return code;
    }
}
