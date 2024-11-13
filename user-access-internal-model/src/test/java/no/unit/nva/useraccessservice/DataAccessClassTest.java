package no.unit.nva.useraccessservice;

import no.unit.nva.database.interfaces.DataAccessClass;
import no.unit.nva.useraccessservice.dao.TermsConditions;
import org.junit.jupiter.api.Test;

import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.*;

class DataAccessClassTest {

    @Test
    void validateBeforePersistSuccessful() {
        DataAccessClass.validateBeforePersist(randomTermsConditions().build());
    }

    @Test
    void validateBeforeFetchSuccessful() {
        DataAccessClass.validateBeforePersist(randomTermsConditions().build());
    }

    @Test
    void validateBeforePersistFailsWhenIdIsNull() {
        var termsConditions = randomTermsConditions().id(null).build();
        assertThrows(IllegalArgumentException.class, () -> DataAccessClass.validateBeforePersist(termsConditions));
    }

    @Test
    void validateBeforePersistFailsWhenModifiedIsNull() {
        var termsConditions = randomTermsConditions().modified(null).build();
        assertThrows(IllegalArgumentException.class, () -> DataAccessClass.validateBeforePersist(termsConditions));
    }

    @Test
    void validateBeforePersistFailsWhenOwnerIsNull() {
        var termsConditions = randomTermsConditions().owner(null).modifiedBy(null).build();
        assertThrows(IllegalArgumentException.class, () -> DataAccessClass.validateBeforePersist(termsConditions));
    }

    private static TermsConditions.Builder randomTermsConditions() {
        var lostInstant = randomInstant();
        return TermsConditions.builder()
                .id(randomUri())
                .type("TermsConditions")
                .created(lostInstant)
                .modified(lostInstant)
                .modifiedBy(randomUri())
                .termsConditionsUri(randomUri());
    }

}