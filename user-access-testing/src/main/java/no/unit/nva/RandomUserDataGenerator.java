package no.unit.nva;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_DOMAIN;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.CRISTIN_PATH;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.core.paths.UriWrapper;

public final class RandomUserDataGenerator {
    
    private RandomUserDataGenerator() {
        // NO-OP
    }
    
    public static URI randomCristinOrgId() {
        return new UriWrapper("https", API_DOMAIN)
                   .addChild(CRISTIN_PATH)
                   .addChild(randomString())
                   .getUri();
    }
    
    public static ViewingScope randomViewingScope() {
        return attempt(() -> ViewingScope.create(Set.of(randomCristinOrgId()), Set.of(randomCristinOrgId())))
                   .orElseThrow();
    }

    public static RoleName randomRoleName() {
        return RoleName.values()[randomInteger(RoleName.values().length)];
    }

    public static RoleName randomRoleNameButNot(RoleName roleName) {
        var values = new ArrayList<>(Arrays.asList(RoleName.values()));
        values.remove(roleName);
        return values.get(randomInteger(values.size()));
    }
}
