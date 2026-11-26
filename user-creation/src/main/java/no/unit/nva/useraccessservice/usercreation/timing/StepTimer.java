package no.unit.nva.useraccessservice.usercreation.timing;

import static java.util.Objects.isNull;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs how long a step of a call took, to tell a slow JVM from a slow dependency. Besides wall
 * time, each line has the CPU time of the whole process (the JIT compiler threads and the HTTP
 * client's own threads included) and the CPU time of the JIT compiler threads alone, all in
 * milliseconds. With one vCPU, CPU time close to wall time means computing and CPU time far below
 * it means waiting. Steps nest, so the CPU times of an outer step include those of its inner steps.
 */
public final class StepTimer {

  static final long NOT_AVAILABLE = -1;

  private static final Logger LOGGER = LoggerFactory.getLogger(StepTimer.class);
  private static final String CALL_START_MESSAGE =
      "Call {} in this JVM: initializationType={} tieredStopAtLevel={} uptimeMs={} cpuMs={}"
          + " jitCpuMs={}";
  private static final String STEP_MESSAGE = "Step {} took wallMs={} cpuMs={} jitCpuMs={}";
  private static final String INITIALIZATION_TYPE_VARIABLE = "AWS_LAMBDA_INITIALIZATION_TYPE";
  private static final String TIERED_STOP_AT_LEVEL_OPTION = "TieredStopAtLevel";
  private static final String UNKNOWN = "unknown";
  private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;
  private static final AtomicInteger CALLS_IN_THIS_JVM = new AtomicInteger();

  private static final Path THREADS_OF_THIS_PROCESS = Path.of("/proc/self/task");
  private static final String THREAD_STAT_FILE = "stat";

  /** Linux cuts thread names to 15 characters, so "C2 CompilerThread0" reads "C2 CompilerThre". */
  private static final List<String> COMPILER_THREAD_NAMES =
      List.of("C1 CompilerThre", "C2 CompilerThre");

  /** Positions of utime and stime among the stat fields after the thread name. */
  private static final int USER_TIME_FIELD = 11;

  private static final int SYSTEM_TIME_FIELD = 12;

  /** Linux reports thread CPU time in clock ticks, 100 per second on Lambda. */
  private static final long MILLISECONDS_PER_CLOCK_TICK = 10;

  private final String stepName;
  private final long wallStartNanoseconds;
  private final long cpuStartMilliseconds;
  private final long jitCpuStartMilliseconds;

  private StepTimer(String stepName) {
    this.stepName = stepName;
    this.wallStartNanoseconds = System.nanoTime();
    this.cpuStartMilliseconds = processCpuMilliseconds();
    this.jitCpuStartMilliseconds = compilerThreadsCpuMilliseconds(THREADS_OF_THIS_PROCESS);
  }

  public static StepTimer start(String stepName) {
    return new StepTimer(stepName);
  }

  public static <T> T time(String stepName, Supplier<T> step) {
    var timer = start(stepName);
    try {
      return step.get();
    } finally {
      timer.finish();
    }
  }

  public static void run(String stepName, Runnable step) {
    var timer = start(stepName);
    try {
      step.run();
    } finally {
      timer.finish();
    }
  }

  /**
   * Numbers the calls this JVM has served, which starts again after Lambda restarts the runtime.
   * The CPU times are totals since the JVM started, so for call 1 they show what init cost.
   */
  public static void logCallStart() {
    LOGGER.info(
        CALL_START_MESSAGE,
        CALLS_IN_THIS_JVM.incrementAndGet(),
        initializationType(),
        tieredStopAtLevel(),
        ManagementFactory.getRuntimeMXBean().getUptime(),
        processCpuMilliseconds(),
        compilerThreadsCpuMilliseconds(THREADS_OF_THIS_PROCESS));
  }

  public void finish() {
    LOGGER.info(
        STEP_MESSAGE,
        stepName,
        (System.nanoTime() - wallStartNanoseconds) / NANOSECONDS_PER_MILLISECOND,
        processCpuMilliseconds() - cpuStartMilliseconds,
        compilerThreadsCpuMilliseconds(THREADS_OF_THIS_PROCESS) - jitCpuStartMilliseconds);
  }

  /**
   * The CPU time of the JIT compiler threads, read from Linux's /proc. The compilation time the JVM
   * reports itself is elapsed time, so a compilation in flight while Lambda keeps the environment
   * frozen counts the whole freeze. Without /proc, as on macOS, this is {@link #NOT_AVAILABLE}.
   */
  static long compilerThreadsCpuMilliseconds(Path threadsDirectory) {
    if (!Files.isDirectory(threadsDirectory)) {
      return NOT_AVAILABLE;
    }
    var clockTicks = 0L;
    try (var threads = Files.newDirectoryStream(threadsDirectory)) {
      for (var thread : threads) {
        clockTicks += compilerThreadClockTicks(thread.resolve(THREAD_STAT_FILE));
      }
    } catch (IOException | DirectoryIteratorException unreadable) {
      return NOT_AVAILABLE;
    }
    return clockTicks * MILLISECONDS_PER_CLOCK_TICK;
  }

  private static long compilerThreadClockTicks(Path statFile) {
    try {
      return compilerThreadClockTicks(Files.readString(statFile));
    } catch (IOException threadEnded) {
      return 0;
    }
  }

  /** A stat line reads "tid (name) state ...", and the name itself may hold spaces. */
  static long compilerThreadClockTicks(String statLine) {
    var nameStart = statLine.indexOf('(');
    var nameEnd = statLine.lastIndexOf(')');
    if (nameStart < 0 || nameEnd < nameStart) {
      return 0;
    }
    if (!isCompilerThread(statLine.substring(nameStart + 1, nameEnd))) {
      return 0;
    }
    var fields = statLine.substring(nameEnd + 1).trim().split("\\s+");
    if (fields.length <= SYSTEM_TIME_FIELD) {
      return 0;
    }
    try {
      return Long.parseLong(fields[USER_TIME_FIELD]) + Long.parseLong(fields[SYSTEM_TIME_FIELD]);
    } catch (NumberFormatException malformed) {
      return 0;
    }
  }

  private static boolean isCompilerThread(String threadName) {
    for (var compilerThreadName : COMPILER_THREAD_NAMES) {
      if (threadName.startsWith(compilerThreadName)) {
        return true;
      }
    }
    return false;
  }

  private static String initializationType() {
    var initializationType = System.getenv(INITIALIZATION_TYPE_VARIABLE);
    return isNull(initializationType) ? UNKNOWN : initializationType;
  }

  /** The value and where it came from, such as DEFAULT or ENVIRON_VAR. */
  private static String tieredStopAtLevel() {
    var diagnostics = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
    if (isNull(diagnostics)) {
      return UNKNOWN;
    }
    try {
      var option = diagnostics.getVMOption(TIERED_STOP_AT_LEVEL_OPTION);
      return option.getValue() + " (" + option.getOrigin() + ")";
    } catch (IllegalArgumentException noSuchOption) {
      return UNKNOWN;
    }
  }

  private static long processCpuMilliseconds() {
    return ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean system
        ? system.getProcessCpuTime() / NANOSECONDS_PER_MILLISECOND
        : NOT_AVAILABLE;
  }
}
