package benchmarks.handin;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import schwaemm.SchwaemmHelper;
import sparkle.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CompleteSparkleBenchmarkAllVariants {

  /*public static void main(String[] args) throws IOException {
    org.openjdk.jmh.Main.main(args);
  }*/

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void sparkle256(ExecutionPlan plan, Blackhole blackhole) {
    int[] state = plan.selectState();
    Sparkle.sparkle256(state);
    blackhole.consume(state);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void sparkle384(ExecutionPlan plan, Blackhole blackhole) {
    int[] state = plan.selectState();
    Sparkle.sparkle384(state);
    blackhole.consume(state);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void sparkle512(ExecutionPlan plan, Blackhole blackhole) {
    int[] state = plan.selectState();
    Sparkle.sparkle512(state);
    blackhole.consume(state);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void sparkle256Slim(ExecutionPlan plan, Blackhole blackhole) {
    int[] state = plan.selectState();
    Sparkle.sparkle256Slim(state);
    blackhole.consume(state);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void sparkle384Slim(ExecutionPlan plan, Blackhole blackhole) {
    int[] state = plan.selectState();
    Sparkle.sparkle384Slim(state);
    blackhole.consume(state);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void sparkle512Slim(ExecutionPlan plan, Blackhole blackhole) {
    int[] state = plan.selectState();
    Sparkle.sparkle512Slim(state);
    blackhole.consume(state);
  }


  /**
   * Selects the next state to be benchmarked.
   *
   * @param index index for lookup
   * @param plan an execution plan
   * @return state
   */


  /** ExecutionPlan class. */
  @State(Scope.Benchmark)
  public static class ExecutionPlan {

    public static final int COUNT = 200;

    int count = 0;

    private int[] selectState() {
      count = (count+1) % ExecutionPlan.COUNT;
      return states[count];
    }

    private final int[][] states = new int[COUNT][Sparkle.maxBranches*2];

    /**
     * Setup method for benchmarks.
     */
    @Setup(Level.Trial)
    public void setUp() {
      //Generate states
      Random random = new Random(1234);
      for (int i = 0; i < COUNT; i++) {
        int[] state = new int[Sparkle.maxBranches*2];
        for (int j = 0; j < Sparkle.maxBranches*2; j++) {
          state[j] = random.nextInt(Integer.MAX_VALUE);
        }
        states[i] = state;
      }
    }
  }
}
