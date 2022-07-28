package no.unit.identityservice.fsproxy;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FsPerson {

    @JsonProperty("personlopenummer")
    private FsIdNumber fsIdNumber;

    public FsIdNumber getFsIdNumber() {
        return fsIdNumber;
    }

    public void setFsIdNumber(FsIdNumber fsIdNumber) {
        this.fsIdNumber = fsIdNumber;
    }


}
