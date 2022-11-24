package no.unit.nva.useraccessservice.usercreation.person;

public class NationalIdentityNumber {
    private static final String MASK_PATTERN = "XXXX";
    private final String nin;

    public NationalIdentityNumber(final String nin) {
        this.nin = nin;
    }

    public String getNin() {
        return nin;
    }

    private static String mask(String ninToMask) {
        if (ninToMask == null || ninToMask.length() < 11) {
            return MASK_PATTERN;
        }
        return ninToMask.substring(0, 2) + MASK_PATTERN + ninToMask.substring(ninToMask.length() - 2);
    }

    @Override
    public String toString() {
        return "NationalIdentityNumber{"
               + "nin='" + mask(nin) + '\''
               + '}';
    }
}
