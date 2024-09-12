package no.unit.nva.useraccessservice.userceation.testing.cristin;

import java.util.concurrent.ThreadLocalRandom;

public class RandomNin {
    public static String randomNin() {
        // Leaving these in place to make it 100% clear what a NIN is here, and no, it isn't this in reality.
        long min = 10_000_000_000L;
        long max = 99_999_999_999L;
        return String.valueOf(ThreadLocalRandom.current().nextLong(min, max + 1));
    }
}
