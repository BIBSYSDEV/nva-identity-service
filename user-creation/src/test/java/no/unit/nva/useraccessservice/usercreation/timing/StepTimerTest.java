package no.unit.nva.useraccessservice.usercreation.timing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StepTimerTest {

  private static final String STEP_LINE_FORMAT =
      "Step %s took wallMs=\\d+ cpuMs=-?\\d+ jitCpuMs=-?\\d+";
  private static final Pattern CALL_START_LINE =
      Pattern.compile(
          "Call (\\d+) in this JVM: initializationType=\\S+ tieredStopAtLevel=\\d+ \\(\\w+\\)"
              + " uptimeMs=\\d+ cpuMs=-?\\d+ jitCpuMs=-?\\d+");

  /** Thread stat lines as Linux writes them, with utime 30 and stime 5 for the C2 thread. */
  private static final String C2_COMPILER_THREAD_STAT =
      "101 (C2 CompilerThre) S 1 1 1 0 -1 4194368 0 0 0 0 30 5 0 0 20 0 1 0 100 0 0";

  private static final String C1_COMPILER_THREAD_STAT =
      "102 (C1 CompilerThre) S 1 1 1 0 -1 4194368 0 0 0 0 7 3 0 0 20 0 1 0 100 0 0";
  private static final String MAIN_THREAD_STAT =
      "103 (java) R 1 1 1 0 -1 4194304 0 0 0 0 500 500 0 0 20 0 1 0 100 0 0";

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

  @Test
  void shouldSumTheCpuTimeOfTheCompilerThreadsOnly(@TempDir Path threads) throws IOException {
    writeThreadStat(threads, "101", C2_COMPILER_THREAD_STAT);
    writeThreadStat(threads, "102", C1_COMPILER_THREAD_STAT);
    writeThreadStat(threads, "103", MAIN_THREAD_STAT);

    assertThat(StepTimer.compilerThreadsCpuMilliseconds(threads)).isEqualTo((35 + 10) * 10);
  }

  @Test
  void shouldSkipAThreadThatEndedWhileReading(@TempDir Path threads) throws IOException {
    writeThreadStat(threads, "101", C2_COMPILER_THREAD_STAT);
    Files.createDirectory(threads.resolve("104"));

    assertThat(StepTimer.compilerThreadsCpuMilliseconds(threads)).isEqualTo(35 * 10);
  }

  @Test
  void shouldTellCompilerCpuTimeAsNotAvailableWithoutProc(@TempDir Path threads) {
    assertThat(StepTimer.compilerThreadsCpuMilliseconds(threads.resolve("missing")))
        .isEqualTo(StepTimer.NOT_AVAILABLE);
  }

  @Test
  void shouldIgnoreAStatLineItCannotRead() {
    assertThat(StepTimer.compilerThreadClockTicks("101 C2 CompilerThre S")).isZero();
    assertThat(StepTimer.compilerThreadClockTicks("101 (C2 CompilerThre) S 1 1")).isZero();
    assertThat(
            StepTimer.compilerThreadClockTicks(
                C2_COMPILER_THREAD_STAT.replace(" 30 5 ", " thirty five ")))
        .isZero();
  }

  private static void writeThreadStat(Path threads, String threadId, String statLine)
      throws IOException {
    var thread = Files.createDirectory(threads.resolve(threadId));
    Files.writeString(thread.resolve("stat"), statLine);
  }
}
