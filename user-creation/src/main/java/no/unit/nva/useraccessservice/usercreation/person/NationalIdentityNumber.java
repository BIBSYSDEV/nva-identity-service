package no.unit.nva.useraccessservice.usercreation.person;

import nva.commons.core.JacocoGenerated;

public final class NationalIdentityNumber {
    private static final String MASK_PATTERN = "XXXX";
    private final String nin;

    private NationalIdentityNumber(final String nin) {
        this.nin = nin;
    }

    public static NationalIdentityNumber fromString(final String nin) {
        return new NationalIdentityNumber(nin);
    }

    public String getNin() {
        return nin;
    }

    @Override
    public String toString() {
        return "NationalIdentityNumber{"
            + "nin='" + mask(nin) + '\''
            + '}';
    }

    private static String mask(String ninToMask) {
        if (ninToMask == null || ninToMask.length() < 11) {
            return MASK_PATTERN;
        }
        return ninToMask.substring(0, 2) + MASK_PATTERN + ninToMask.substring(ninToMask.length() - 2);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        return this == o  || o instanceof NationalIdentityNumber that && nin.equals(that.nin);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return nin.hashCode();
    }
}
