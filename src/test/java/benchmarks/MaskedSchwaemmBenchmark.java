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

public class MaskedSchwaemmBenchmark {

  public static void main(String[] args) throws IOException {
    org.openjdk.jmh.Main.main(args);
  }

  /**
   * Benchmarks implementation of boolean masked sparkle 256.
   *
   * @param plan an execution plan
   */
  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemm128128BooleanEncrypt(ExecutionPlan plan) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState(i % 34, plan);
      plan.schwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(), data.key(),
          data.nonce());
    }
  }

  /**
   * Benchmarks implementation of conversion masked sparkle 256.
   *
   * @param plan an execution plan
   */
  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemm128128ConversionEncrypt(ExecutionPlan plan) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState(i % 34, plan);
      plan.schwaemmConversion.encryptAndTag(data.message(), data.cipher(), data.associate(), data.key(),
          data.nonce());
    }
  }

  /**
   * Benchmarks implementation of boolean masked sparkle 256.
   *
   * @param plan an execution plan
   */
  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemm128128BooleanEncryptDecrypt(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState(i % 34, plan);
      plan.schwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(), data.key(),
          data.nonce());
      blackhole.consume(
          plan.schwaemmConversion.decryptAndVerify(data.cipher(), data.associate(), data.key(),
              data.nonce()));
    }
  }

  /**
   * Benchmarks implementation of conversion masked sparkle 256.
   *
   * @param plan an execution plan
   */
  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemm128128ConversionEncryptDecrypt(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper.MaskedData data = selectState(i % 34, plan);
      plan.schwaemmConversion.encryptAndTag(data.message(), data.cipher(), data.associate(), data.key(),
          data.nonce());
      blackhole.consume(
          plan.schwaemmConversion.decryptAndVerify(data.cipher(), data.associate(), data.key(),
              data.nonce()));
    }
  }

  /**
   * Selects the next state to be benchmarked.
   *
   * @param index index for lookup
   * @param plan an execution plan
   * @return state
   */
  private SchwaemmHelper.MaskedData selectState(int index, ExecutionPlan plan) {
    return plan.data[index % ExecutionPlan.COUNT];
  }

  /** ExecutionPlan class. */
  @State(Scope.Benchmark)
  public static class ExecutionPlan {

    private final SchwaemmMasked schwaemm = new SchwaemmMasked(SchwaemmType.S128128, new MaskedSparkleBoolean());

    private final SchwaemmMasked schwaemmConversion = new SchwaemmMasked(SchwaemmType.S128128, new MaskedSparkleFirstOrder());

    public static final int COUNT = 13;

    @Param({"10000"})
    private int iterations;

    private static Random random = new Random(1234);

    private final SchwaemmHelper.MaskedData[] data = new SchwaemmHelper.MaskedData[COUNT];

    /** Setup method for benchmarks. */
    @Setup(Level.Invocation)
    public void setUp() {
      for (int i = 0; i < COUNT; i++) {
        data[i] = SchwaemmHelper.prepareBenchmarkMasked(SchwaemmType.S128128, random, 2);
      }
    }
  }
}
