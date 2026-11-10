package no.unit.nva.cognito.inbound;

import static no.unit.nva.cognito.inbound.InboundFederationHandler.ADMITTED_ON_LINK_FILTER_TERM;
import static no.unit.nva.cognito.inbound.InboundFederationHandler.LINK_REWRITTEN_FILTER_TERM;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The metric filters in the template match two of the handler's log lines. A term changed in one
 * place and not the other would silence an alarm, so the two are held together here.
 */
class InboundFederationMetricFiltersTest {

  /** Gradle runs the tests from the module directory; the template is one level up. */
  private static final Path TEMPLATE = Path.of("..", "template.yaml");

  @ParameterizedTest
  @ValueSource(strings = {LINK_REWRITTEN_FILTER_TERM, ADMITTED_ON_LINK_FILTER_TERM})
  void shouldFilterOnTheTermAsTheHandlerLogsIt(String term) throws IOException {
    assertThat(Files.readString(TEMPLATE)).contains("FilterPattern: '\"" + term + "\"'");
  }
}
