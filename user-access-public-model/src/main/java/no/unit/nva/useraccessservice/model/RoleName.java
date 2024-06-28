package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import nva.commons.core.JacocoGenerated;

public enum RoleName {

    PUBLISHING_CURATOR("Publishing-Curator"),
    DOI_CURATOR("Doi-Curator"),
    NVI_CURATOR("Nvi-Curator"),
    SUPPORT_CURATOR("Support-Curator"),
    INSTITUTION_ADMIN("Institution-admin"),
    INTERNAL_IMPORTER("Internal-importer"),
    THESIS_CURATOR("Curator-thesis"),
    EMBARGO_THESIS_CURATOR("Curator-thesis-embargo"),
    APPLICATION_ADMIN("App-admin"),
    EDITOR("Editor"),
    CREATOR("Creator"),
    @Deprecated
    DEPRECATED_NVI_CURATOR("Nvi-curator"),
    @Deprecated
    DEPRECATED_CURATOR("Curator");

    private final String value;

    RoleName(String value) {
        this.value = value;
    }

    public static RoleName fromValue(String value) {
        return Arrays.stream(RoleName.values())
                   .filter(entry -> entry.getValue().equals(value))
                   .findFirst()
                   .orElseThrow();
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JacocoGenerated
    @JsonIgnore
    public boolean isDeprecated() {
        return this.equals(DEPRECATED_NVI_CURATOR) || this.equals(DEPRECATED_CURATOR);
    }
}