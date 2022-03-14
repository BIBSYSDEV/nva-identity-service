package no.unit.nva.cognito;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.cognito.cristin.person.CristinRole;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;

public final class CristinDataGenerator {

    private CristinDataGenerator() {

    }

    public static CristinPersonResponse createCristinResponse(NationalIdentityNumber nationalIdentityNumber) {
        return CristinPersonResponse.builder()
            .withNin(nationalIdentityNumber)
            .withAffiliations(createAffiliations())
            .withFirstName(randomString())
            .withLastName(randomString())
            .build();
    }

    private static CristinAffiliation randomAffiliation() {
        return CristinAffiliation.builder()
            .withActive(randomBoolean())
            .withOrganization(randomUri())
            .withRole(randomCristinRole())
            .build();
    }

    private static CristinRole randomCristinRole() {
        return CristinRole.builder().withId(randomUri()).build();
    }

    private static int smallNumber() {
        return 2 + randomInteger(10);
    }

    private static List<CristinAffiliation> createAffiliations() {
        return IntStream.range(0, smallNumber()).boxed().map(ignored -> randomAffiliation())
            .collect(Collectors.toList());
    }
}
