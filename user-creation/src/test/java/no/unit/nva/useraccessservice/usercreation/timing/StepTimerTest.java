package no.unit.nva.useraccessservice.usercreation.timing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StepTimerTest {

  private static final String STEP_LINE_FORMAT =
      "Step %s took wallMs=\\d+ cpuMs=-?\\d+ jitMs=-?\\d+";
  private static final Pattern CALL_START_LINE =
      Pattern.compile(
          "Call (\\d+) in this JVM: initializationType=\\S+ tieredStopAtLevel=\\d+ \\(\\w+\\)"
              + " uptimeMs=\\d+ cpuMs=-?\\d+ jitMs=-?\\d+");

  private LogRecorder logRecorder;

  @BeforeEach
  void init() {
    logRecorder = LogRecorder.forClass(StepTimer.class);
  }

  @Test
  void shouldReturnTheResultOfAStepAndLogItsTiming() {
    var result = StepTimer.time("some-step", () -> "result");

    assertThat(result).isEqualTo("result");
    assertThat(logRecorder.messages())
        .singleElement()
        .asString()
        .matches(STEP_LINE_FORMAT.formatted("some-step"));
  }

  @Test
  void shouldLogTheTimingOfAStepThatFails() {
    Runnable failingStep =
        () -> {
          throw new IllegalStateException("failed");
        };

    assertThatThrownBy(() -> StepTimer.run("failing-step", failingStep))
        .isInstanceOf(IllegalStateException.class);
    assertThat(logRecorder.messages())
        .singleElement()
        .asString()
        .matches(STEP_LINE_FORMAT.formatted("failing-step"));
  }

  @Test
  void shouldLogTheTimingOfAStepFinishedByTheCaller() {
    var timer = StepTimer.start("cristin GET /persons/resolve");

    timer.finish();

    assertThat(logRecorder.messages())
        .singleElement()
        .asString()
        .matches(STEP_LINE_FORMAT.formatted("cristin GET /persons/resolve"));
  }

  @Test
  void shouldNumberTheCallsThisJvmHasServed() {
    StepTimer.logCallStart();
    StepTimer.logCallStart();

    var callNumbers =
        logRecorder.messages().stream()
            .map(CALL_START_LINE::matcher)
            .filter(Matcher::matches)
            .map(matcher -> Integer.parseInt(matcher.group(1)))
            .toList();
    assertThat(callNumbers).hasSize(2);
    assertThat(callNumbers.get(1)).isEqualTo(callNumbers.get(0) + 1);
  }

  @Test
  void shouldTellTheInitializationTypeAsUnknownOutsideLambda() {
    StepTimer.logCallStart();

    assertThat(logRecorder.messages())
        .singleElement()
        .asString()
        .contains("initializationType=unknown");
  }
}
