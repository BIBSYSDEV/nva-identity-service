package no.unit.nva;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessmanagement.constants.ServiceConstants.API_HOST;
import static no.unit.nva.useraccessmanagement.constants.ServiceConstants.CRISTIN_PATH;
import java.net.URI;
import java.util.Set;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;

public class RandomUserDataGenerator {

    public  static URI randomCristinOrgId() {
        return new UriWrapper("https", API_HOST)
            .addChild(CRISTIN_PATH)
            .addChild(randomString())
            .getUri();
    }

    public static ViewingScope randomViewingScope() throws BadRequestException {
        return new ViewingScope(Set.of(randomCristinOrgId()), Set.of(randomCristinOrgId()));
    }

}
