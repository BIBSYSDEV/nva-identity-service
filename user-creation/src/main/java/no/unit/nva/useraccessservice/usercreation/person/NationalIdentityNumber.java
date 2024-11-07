package no.unit.nva.useraccessservice.usercreation.person;

public final class NationalIdentityNumber {
    private static final String MASK_PATTERN = "XXXX";
    private final String nin;

    private NationalIdentityNumber(final String nin) {
        this.nin = nin;
    }

    public static NationalIdentityNumber fromString(final String nin) {
        return new NationalIdentityNumber(nin);
    }

    private static String mask(String ninToMask) {
        if (ninToMask == null || ninToMask.length() < 11) {
            return MASK_PATTERN;
        }
        return ninToMask.substring(0, 2) + MASK_PATTERN + ninToMask.substring(ninToMask.length() - 2);
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
}
