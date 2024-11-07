package no.unit.nva.customer.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class ApplicationDomainTest {

    public static Stream<Arguments> nullAndEmptyUriProvider() {
        return Stream.of(null, Arguments.of(URI.create("")));
    }

    @ParameterizedTest
    @MethodSource("nullAndEmptyUriProvider")
    void shouldMapEmptyNullValuesToNvaApplicationDomain(URI uri) {
        var actual = ApplicationDomain.fromUri(uri);
        assertThat(actual, is((equalTo(ApplicationDomain.NVA))));
    }

}