package no.unit.nva.cognito.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClientPrimingTest extends InboundFederationHandlerTestBase {

  private static final String PROVISIONED_CONCURRENCY = "provisioned-concurrency";
  private static final String STEP_FAILED = "failed, continuing without it";

  @ParameterizedTest
  @ValueSource(strings = {PROVISIONED_CONCURRENCY, "snap-start"})
  void shouldPrimeEveryClientWhenTheInstanceIsInitializedAheadOfLogins(String initializationType) {
    var registry = mock(PersonRegistry.class);

    primingWith(registry).primeFor(initializationType);

    verify(registry).prime();
    assertThat(logRecorder.asString())
        .contains("Primed the clients during " + initializationType)
        .contains("person registry=", "link table=", "customer table=", "user table=")
        .doesNotContain(STEP_FAILED);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"on-demand"})
  void shouldNotPrimeWhenALoginIsWaitingForTheInit(String initializationType) {
    var registry = mock(PersonRegistry.class);

    primingWith(registry).primeFor(initializationType);

    verifyNoInteractions(registry);
    assertThat(logRecorder.asString()).doesNotContain("Primed");
  }

  @Test
  void shouldPrimeTheOtherClientsWhenOneFails() {
    var registry = mock(PersonRegistry.class);
    doThrow(new IllegalStateException("Cristin is down")).when(registry).prime();

    assertThatCode(() -> primingWith(registry).primeFor(PROVISIONED_CONCURRENCY))
        .doesNotThrowAnyException();

    assertThat(logRecorder.asString())
        .contains("Priming the person registry " + STEP_FAILED)
        .contains("user table=");
  }

  private ClientPriming primingWith(PersonRegistry registry) {
    return new ClientPriming(registry, identityService, customerService, clock);
  }
}
