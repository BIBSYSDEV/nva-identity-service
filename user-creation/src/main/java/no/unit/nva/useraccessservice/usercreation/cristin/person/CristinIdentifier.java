package no.unit.nva.useraccessservice.usercreation.cristin.person;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;

@JacocoGenerated
public class CristinIdentifier {

    public static final String CRISTIN_IDENTIFIER_TYPE = "CristinIdentifier";
    public static final String IGNORED = "ignored";

    private String type;
    private String value;

    @JacocoGenerated
    public CristinIdentifier() {

    }

    public CristinIdentifier(String value) {
        this.type = CRISTIN_IDENTIFIER_TYPE;
        this.value = value;
    }

    public static CristinIdentifier fromCristinId(URI id) {
        return Try.of(id)
            .map(UriWrapper::fromUri)
            .map(UriWrapper::getLastPathElement)
            .map(CristinIdentifier::new)
            .orElseThrow();
    }

    public static List<CristinIdentifier> selectFromCandidates(List<CristinIdentifier> candidates) {
        return Optional.ofNullable(candidates)
            .stream()
            .flatMap(Collection::stream)
            .filter(identifier -> CristinIdentifier.CRISTIN_IDENTIFIER_TYPE.equals(identifier.getType()))
            .collect(Collectors.toList());
    }

    @JacocoGenerated
    public String getType() {
        return type;
    }

    @JacocoGenerated
    public void setType(String type) {
        this.type = CRISTIN_IDENTIFIER_TYPE.equals(type) ? CRISTIN_IDENTIFIER_TYPE : IGNORED;
    }

    @JacocoGenerated
    public String getValue() {
        return value;
    }

    @JacocoGenerated
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CristinIdentifier)) {
            return false;
        }
        CristinIdentifier that = (CristinIdentifier) o;
        return Objects.equals(getType(), that.getType()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
