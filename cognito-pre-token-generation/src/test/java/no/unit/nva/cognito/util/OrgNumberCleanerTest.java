package no.unit.nva.cognito.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OrgNumberCleanerTest {

    public static final String COUNTRY_PREFIX = "NO";
    public static final String ORG_NUMBER = "1234567890";

    @Test
    public void removePrefixReturnsOrgNumberWhenOnOrgNumberWithCountryPrefix() {
        String orgNumberWithoutCountryPrefix = OrgNumberCleaner.removeCountryPrefix(COUNTRY_PREFIX + ORG_NUMBER);
        assertEquals(ORG_NUMBER, orgNumberWithoutCountryPrefix);
    }

    @Test
    public void removePrefixReturnsOrgNumberOnOrgNumberWithoutCountryPrefix() {
        String orgNumberWithoutCountryPrefix = OrgNumberCleaner.removeCountryPrefix(ORG_NUMBER);
        assertEquals(ORG_NUMBER, orgNumberWithoutCountryPrefix);
    }

}
