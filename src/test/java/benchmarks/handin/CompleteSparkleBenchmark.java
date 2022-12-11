package benchmarks.handin;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import schwaemm.SchwaemmHelper;
import sparkle.*;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CompleteSparkleBenchmark {

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
  @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void maskedFirstOrderSparkle(ExecutionPlan plan, Blackhole blackhole) {
      int[][] state = plan.selectStateFirstOrder();
      plan.firstOrderSparkle.sparkle256(state);
      blackhole.consume(state);
    }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void maskedBooleanSparkleFirstOder(ExecutionPlan plan, Blackhole blackhole) {
    int[][] state = plan.selectStateFirstOrder();
    plan.booleanSparkle.sparkle256(state);
    blackhole.consume(state);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void maskedGoubinSparkle(ExecutionPlan plan, Blackhole blackhole) {
    int[][] state = plan.selectStateFirstOrder();
    plan.goubinSparkle.sparkle256(state);
    blackhole.consume(state);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void maskedKoggeSparkle(ExecutionPlan plan, Blackhole blackhole) {
    int[][] state = plan.selectStateFirstOrder();
    plan.koggeSparkle.sparkle256(state);
    blackhole.consume(state);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void maskedHigherOrderSparkle(ExecutionPlan plan, Blackhole blackhole) {
    int[][] state = plan.selectStateHigherOrder3();
    plan.higherOrderSparkle.sparkle256(state);
    blackhole.consume(state);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void maskedBooleanSparkleHigherOder(ExecutionPlan plan, Blackhole blackhole) {
    int[][] state = plan.selectStateHigherOrder3();
    plan.booleanSparkle.sparkle256(state);
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

    private final MaskedSparkle firstOrderSparkle = new MaskedSparkleFirstOrder();
    private final MaskedSparkle goubinSparkle = new MaskedSparkleGoubin();
    private final MaskedSparkle higherOrderSparkle = new MaskedSparkleHigherOrder();
    private final MaskedSparkle booleanSparkle = new MaskedSparkleBoolean();
    private final MaskedSparkle koggeSparkle = new MaskedSparkleKoggeStone();

    public static final int COUNT = 200;

    int count = 0;

    private int[] selectState() {
      count = (count+1) % ExecutionPlan.COUNT;
      return states[count];
    }

    private int[][] selectStateFirstOrder() {
      count = (count+1) % ExecutionPlan.COUNT;
      return statesFirstOrder[count];
    }

    private int[][] selectStateHigherOrder3() {
      count = (count+1) % ExecutionPlan.COUNT;
      return statesHigherOrder3[count];
    }


    private final int[][] states = new int[COUNT][Sparkle.maxBranches];
    private final int[][][] statesFirstOrder = new int[COUNT][2][Sparkle.maxBranches];

    private final int[][][] statesHigherOrder3 = new int[COUNT][3][Sparkle.maxBranches];

    /**
     * Setup method for benchmarks.
     */
    @Setup(Level.Trial)
    public void setUp() {
      //Generate states
      Random random = new Random(1234);
      for (int i = 0; i < COUNT; i++) {
        int[] state = new int[Sparkle.maxBranches];
        for (int j = 0; j < Sparkle.maxBranches; j++) {
          state[j] = random.nextInt(Integer.MAX_VALUE);
        }
        states[i] = state;
      }

      //Setup first order states
      for (int i = 0; i < COUNT; i++) {
        statesFirstOrder[i] = SchwaemmHelper.maskIntArray(states[i], 2);
      }
      for (int i = 0; i < COUNT; i++) {
        statesHigherOrder3[i] = SchwaemmHelper.maskIntArray(states[i], 3);
      }
    }
  }
}
