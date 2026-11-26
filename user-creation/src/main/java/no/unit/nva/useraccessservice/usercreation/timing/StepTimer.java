package no.unit.nva.useraccessservice.usercreation.timing;

import static java.util.Objects.isNull;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs how long a step of a call took, to tell a slow JVM from a slow dependency. Besides wall
 * time, each line has the CPU time of the whole process (the JIT compiler threads and the HTTP
 * client's own threads included) and the time spent in JIT compilation, all in milliseconds. With
 * one vCPU, CPU time close to wall time means computing and CPU time far below it means waiting.
 * Steps nest, so the CPU and JIT time of an outer step include those of its inner steps.
 */
public final class StepTimer {

  private static final Logger LOGGER = LoggerFactory.getLogger(StepTimer.class);
  private static final String CALL_START_MESSAGE =
      "Call {} in this JVM: initializationType={} tieredStopAtLevel={} uptimeMs={} cpuMs={}"
          + " jitMs={}";
  private static final String STEP_MESSAGE = "Step {} took wallMs={} cpuMs={} jitMs={}";
  private static final String INITIALIZATION_TYPE_VARIABLE = "AWS_LAMBDA_INITIALIZATION_TYPE";
  private static final String TIERED_STOP_AT_LEVEL_OPTION = "TieredStopAtLevel";
  private static final String UNKNOWN = "unknown";
  private static final long NOT_AVAILABLE = -1;
  private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;
  private static final AtomicInteger CALLS_IN_THIS_JVM = new AtomicInteger();

  private final String stepName;
  private final long wallStartNanoseconds;
  private final long cpuStartMilliseconds;
  private final long jitStartMilliseconds;

  private StepTimer(String stepName) {
    this.stepName = stepName;
    this.wallStartNanoseconds = System.nanoTime();
    this.cpuStartMilliseconds = processCpuMilliseconds();
    this.jitStartMilliseconds = jitMilliseconds();
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
   * The CPU and JIT time are totals since the JVM started, so for call 1 they show what init cost.
   */
  public static void logCallStart() {
    LOGGER.info(
        CALL_START_MESSAGE,
        CALLS_IN_THIS_JVM.incrementAndGet(),
        initializationType(),
        tieredStopAtLevel(),
        ManagementFactory.getRuntimeMXBean().getUptime(),
        processCpuMilliseconds(),
        jitMilliseconds());
  }

  public void finish() {
    LOGGER.info(
        STEP_MESSAGE,
        stepName,
        (System.nanoTime() - wallStartNanoseconds) / NANOSECONDS_PER_MILLISECOND,
        processCpuMilliseconds() - cpuStartMilliseconds,
        jitMilliseconds() - jitStartMilliseconds);
  }

  private static String initializationType() {
    var initializationType = System.getenv(INITIALIZATION_TYPE_VARIABLE);
    return isNull(initializationType) ? UNKNOWN : initializationType;
  }

  /** The value and where it came from, such as VM_CREATION or ENVIRON_VAR. */
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

  private static long jitMilliseconds() {
    var compiler = ManagementFactory.getCompilationMXBean();
    return isNull(compiler) || !compiler.isCompilationTimeMonitoringSupported()
        ? NOT_AVAILABLE
        : compiler.getTotalCompilationTime();
  }
}
