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
import schwaemm.Schwaemm;
import schwaemm.SchwaemmHelper;
import schwaemm.SchwaemmType;

public class SchwaemmBenchmark {

  public static void main(String[] args) throws IOException {
    org.openjdk.jmh.Main.main(args);
  }


  @Fork(value = 1, warmups = 1)
  @Benchmark
  public void schwaemm128128Encrypt(ExecutionPlan plan, Blackhole blackhole) {
    for (int i = plan.iterations; i > 0; i--) {
      SchwaemmHelper data = selectData(i % 34, plan);
      plan.schwaemm.encryptAndTag(data.message(), data.cipherJava(), data.associate(), data.key(),
          data.nonce());
      blackhole.consume(data);
    }
  }

  private SchwaemmHelper selectData(int index, ExecutionPlan plan) {
    return plan.data[index % ExecutionPlan.COUNT];
  }

  /** ExecutionPlan class. */
  @State(Scope.Benchmark)
  public static class ExecutionPlan {

    public static final int COUNT = 13;

    private final Schwaemm schwaemm = new Schwaemm(SchwaemmType.S128128);

    @Param({"10000"})
    private int iterations;

    private final SchwaemmHelper[] data = new SchwaemmHelper[COUNT];

    /** Setup method for benchmarks. */
    @Setup(Level.Invocation)
    public void setUp() {
      Random random = new Random(1234);
      for (int i = 0; i < COUNT; i++) {
        data[i] = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 0, random, 32);
      }
    }
  }
}
