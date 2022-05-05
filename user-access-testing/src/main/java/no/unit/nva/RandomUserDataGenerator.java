package no.unit.nva;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_HOST;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.CRISTIN_PATH;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Set;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.core.paths.UriWrapper;

public class RandomUserDataGenerator {

    public static URI randomCristinOrgId() {
        return new UriWrapper("https", API_HOST)
            .addChild(CRISTIN_PATH)
            .addChild(randomString())
            .getUri();
    }

    public static ViewingScope randomViewingScope() {
        return attempt(() -> ViewingScope.create(Set.of(randomCristinOrgId()), Set.of(randomCristinOrgId())))
            .orElseThrow();
    }
}
