package no.unit.nva.cognito.util;

public final class OrgNumberCleaner {

    public static final String NON_DIGITS_PATTERN = "[^\\d.]";
    public static final String EMPTY_STRING = "";

    private OrgNumberCleaner() {

    }

    public static String removeCountryPrefix(String orgNumber) {
        return orgNumber.replaceAll(NON_DIGITS_PATTERN, EMPTY_STRING);
    }

}
