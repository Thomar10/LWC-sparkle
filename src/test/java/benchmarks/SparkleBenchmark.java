package benchmarks;

import java.io.IOException;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import sparkle.Sparkle;

public class SparkleBenchmark {

  public static void main(String[] args) throws IOException {
    org.openjdk.jmh.Main.main(args);
  }

  /**
   * Benchmarks implementation of sparkle 256.
   *
   * @param plan an execution plan
   * @param blackhole a black hole
   */
  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void sparkle256(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      int[] state = selectState(i % 34, plan);
      Sparkle.sparkle256(state);
    }
  }

  /**
   * Selects the next state to be benchmarked.
   *
   * @param index index for lookup
   * @param plan an execution plan
   * @return state
   */
  private int[] selectState(int index, ExecutionPlan plan) {
    return plan.states[index % ExecutionPlan.COUNT];
  }

  /** ExecutionPlan class. */
  @State(Scope.Benchmark)
  public static class ExecutionPlan {

    public static final int COUNT = 13;

    @Param({"10000"})
    private int iterations;

    private final int[][] states = new int[COUNT][Sparkle.maxBranches];

    /** Setup method for benchmarks. */
    @Setup(Level.Invocation)
    public void setUp() {
      Random random = new Random(1234);
      for (int i = 0; i < COUNT; i++) {
        int[] state = new int[Sparkle.maxBranches];
        for (int j = 0; j < Sparkle.maxBranches; j++) {
          state[j] = random.nextInt(Integer.MAX_VALUE);
        }
        states[i] = state;
      }
    }
  }
}
