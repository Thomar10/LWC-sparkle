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
import schwaemm.SchwaemmHelper;
import schwaemm.SchwaemmMasked;
import schwaemm.SchwaemmType;
import sparkle.MaskedSparkleBoolean;
import sparkle.MaskedSparkleFirstOrder;
import sparkle.MaskedSparkleHigherOrder;
import sparkle.MaskedSparkleKoggeStone;

public class MaskedSchwaemmBenchmark {

  public static void main(String[] args) throws IOException {
    org.openjdk.jmh.Main.main(args);
  }


  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemmEncryptBoolean(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState(i % 34, plan);
      plan.booleanSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
          data.key(),
          data.nonce());
      blackhole.consume(data);
    }
  }

  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemmEncryptFirstOrder(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState(i % 34, plan);
      plan.firstOrderSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
          data.key(),
          data.nonce());
      blackhole.consume(data);

    }
  }

  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemmEncryptKoggeStone(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState(i % 34, plan);
      plan.koggeStoneSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
          data.key(),
          data.nonce());
      blackhole.consume(data);
    }
  }

  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemmEncryptHigherOrder(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState(i % 34, plan);
      plan.higherOrderSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
          data.key(),
          data.nonce());
      blackhole.consume(data);

    }
  }


  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemmEncryptHigherOrder4(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState4(i % 34, plan);
      plan.higherOrderSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
          data.key(),
          data.nonce());
      blackhole.consume(data);

    }
  }

  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemmEncryptBoolean4(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState4(i % 34, plan);
      plan.higherOrderSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
          data.key(),
          data.nonce());
      blackhole.consume(data);

    }
  }


  /**
   * Selects the next state to be benchmarked.
   *
   * @param index index for lookup
   * @param plan  an execution plan
   * @return state
   */
  private SchwaemmHelper.MaskedData selectState(int index, ExecutionPlan plan) {
    return plan.data[index % ExecutionPlan.COUNT];
  }

  private SchwaemmHelper.MaskedData selectState4(int index, ExecutionPlan plan) {
    return plan.data4[index % ExecutionPlan.COUNT];
  }

  /**
   * ExecutionPlan class.
   */
  @State(Scope.Benchmark)
  public static class ExecutionPlan {

    private final SchwaemmMasked booleanSchwaemm = new SchwaemmMasked(SchwaemmType.S128128,
        new MaskedSparkleBoolean());
    private final SchwaemmMasked firstOrderSchwaemm = new SchwaemmMasked(SchwaemmType.S128128,
        new MaskedSparkleFirstOrder());
    private final SchwaemmMasked koggeStoneSchwaemm = new SchwaemmMasked(SchwaemmType.S128128,
        new MaskedSparkleKoggeStone());
    private final SchwaemmMasked higherOrderSchwaemm = new SchwaemmMasked(SchwaemmType.S128128,
        new MaskedSparkleHigherOrder());

    public static final int COUNT = 13;

    @Param({"10000"})
    private int iterations;

    private static Random random = new Random(1234);

    private final SchwaemmHelper.MaskedData[] data = new SchwaemmHelper.MaskedData[COUNT];
    private final SchwaemmHelper.MaskedData[] data4 = new SchwaemmHelper.MaskedData[COUNT];

    /** Setup method for benchmarks. */
    @Setup(Level.Invocation)
    public void setUp() {
      for (int i = 0; i < COUNT; i++) {
        data[i] = SchwaemmHelper.prepareBenchmarkMasked(SchwaemmType.S128128, random, 2);
        data4[i] = SchwaemmHelper.prepareBenchmarkMasked(SchwaemmType.S128128, random, 4);
      }
    }
  }
}
