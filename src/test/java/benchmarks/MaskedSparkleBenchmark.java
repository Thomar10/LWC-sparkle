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
import sparkle.MaskedSparkle;
import sparkle.MaskedSparkleBoolean;
import sparkle.MaskedSparkleFirstOrder;
import sparkle.MaskedSparkleHigherOrder;
import sparkle.MaskedSparkleKoggeStone;
import sparkle.Sparkle;

public class MaskedSparkleBenchmark {

  public static void main(String[] args) throws IOException {
    org.openjdk.jmh.Main.main(args);
  }


  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void maskedFirstOrderSparkle(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      int[][] state = selectState(i % 34, plan);
      plan.firstOrderSparkle.sparkle256(state);
      blackhole.consume(state);
    }
  }

  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void maskedHigherOrderSparkle(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      int[][] state = selectState(i % 34, plan);
      plan.higherOrderSparkle.sparkle256(state);
      blackhole.consume(state);
    }
  }


  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void maskedBooleanSparkle(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      int[][] state = selectState(i % 34, plan);
      plan.booleanSparkle.sparkle256(state);
      blackhole.consume(state);
    }
  }


  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void maskedKoggeSparkle(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      int[][] state = selectState(i % 34, plan);
      plan.koggeSparkle.sparkle256(state);
      blackhole.consume(state);
    }
  }

  private int[][] selectState(int index, ExecutionPlan plan) {
    return plan.states[index % ExecutionPlan.COUNT];
  }

  /**
   * ExecutionPlan class.
   */
  @State(Scope.Benchmark)
  public static class ExecutionPlan {

    private final MaskedSparkle firstOrderSparkle = new MaskedSparkleFirstOrder();
    private final MaskedSparkle higherOrderSparkle = new MaskedSparkleHigherOrder();
    private final MaskedSparkle booleanSparkle = new MaskedSparkleBoolean();
    private final MaskedSparkle koggeSparkle = new MaskedSparkleKoggeStone();

    public static final int COUNT = 13;

    @Param({"10000"})
    private int iterations;

    private static Random random = new Random(1234);

    private final int[][][] states = new int[COUNT][][];

    /** Setup method for benchmarks. */
    @Setup(Level.Invocation)
    public void setUp() {
      for (int i = 0; i < COUNT; i++) {
        states[i] = generateStateShares(2);
      }
    }

    static int[][] generateStateShares(int order) {
      int shareLength = Sparkle.maxBranches;

      int[] state = generateRandomShare(shareLength);
      int[][] stateShares = new int[order][shareLength];

      System.arraycopy(state, 0, stateShares[0], 0, shareLength);

      for (int i = 1; i < order; i++) {
        stateShares[i] = generateRandomShare(shareLength);
        maskShare(stateShares[0], stateShares[i]);
      }

      return stateShares;
    }

    static void maskShare(int[] share, int[] mask) {
      for (int i = 0; i < share.length; i++) {
        share[i] ^= mask[i];
      }
    }

    static int[] generateRandomShare(int length) {
      int[] share = new int[length];

      for (int i = 0; i < length; i++) {
        share[i] = random.nextInt(Integer.MAX_VALUE);
      }
      return share;
    }
  }
}
