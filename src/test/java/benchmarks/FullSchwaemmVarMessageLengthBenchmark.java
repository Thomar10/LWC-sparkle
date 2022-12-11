package benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import schwaemm.Schwaemm;
import schwaemm.SchwaemmHelper;
import schwaemm.SchwaemmMasked;
import schwaemm.SchwaemmType;
import sparkle.MaskedSparkleBoolean;
import sparkle.MaskedSparkleFirstOrder;
import sparkle.MaskedSparkleHigherOrder;
import sparkle.MaskedSparkleKoggeStone;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class FullSchwaemmVarMessageLengthBenchmark {


  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void schwaemmNormal(ExecutionPlan plan, Blackhole blackhole) {
    SchwaemmHelper data = plan.selectState();
    plan.schwaemmNormal.encryptAndTag(data.message(), data.cipherJava(), data.associate(),
            data.key(),
            data.nonce());
    blackhole.consume(data);
  }


  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void schwaemmEncryptBooleanFirstOrderInput(ExecutionPlan plan, Blackhole blackhole) {
    SchwaemmHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.booleanSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
        data.key(),
        data.nonce());
    blackhole.consume(data);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void schwaemmEncryptFirstOrder(ExecutionPlan plan, Blackhole blackhole) {
    SchwaemmHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.firstOrderSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
        data.key(),
        data.nonce());
    blackhole.consume(data);

  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void schwaemmEncryptKoggeStone(ExecutionPlan plan, Blackhole blackhole) {
    SchwaemmHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.koggeStoneSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
        data.key(),
        data.nonce());
    blackhole.consume(data);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 20000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void schwaemmEncryptHigherOrderWithFirstOrderInput(ExecutionPlan plan, Blackhole blackhole) {
    SchwaemmHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.higherOrderSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
        data.key(),
        data.nonce());
    blackhole.consume(data);

  }


  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 20000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void schwaemmEncryptHigherOrder3(ExecutionPlan plan, Blackhole blackhole) {
    SchwaemmHelper.MaskedData data = plan.selectStateHigherOrder3();
    plan.higherOrderSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
        data.key(),
        data.nonce());
    blackhole.consume(data);


  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 20000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void schwaemmEncryptBoolean3(ExecutionPlan plan, Blackhole blackhole) {
    SchwaemmHelper.MaskedData data = plan.selectStateHigherOrder3();
    plan.higherOrderSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
        data.key(),
        data.nonce());
    blackhole.consume(data);
  }

  /**
   * ExecutionPlan class.
   */
  @State(Scope.Benchmark)
  public static class ExecutionPlan {


    private final Schwaemm schwaemmNormal = new Schwaemm(SchwaemmType.S128128);
    private final SchwaemmMasked booleanSchwaemm = new SchwaemmMasked(SchwaemmType.S128128,
        new MaskedSparkleBoolean());
    private final SchwaemmMasked firstOrderSchwaemm = new SchwaemmMasked(SchwaemmType.S128128,
        new MaskedSparkleFirstOrder());
    private final SchwaemmMasked koggeStoneSchwaemm = new SchwaemmMasked(SchwaemmType.S128128,
        new MaskedSparkleKoggeStone());
    private final SchwaemmMasked higherOrderSchwaemm = new SchwaemmMasked(SchwaemmType.S128128,
        new MaskedSparkleHigherOrder());

    int count;

    private SchwaemmHelper selectState() {
      count = (count+1) % CompleteSparkleBenchmark.ExecutionPlan.COUNT;
      return data[count];
    }

    private SchwaemmHelper.MaskedData selectStateFirstOrder() {
      count = (count+1) % CompleteSparkleBenchmark.ExecutionPlan.COUNT;
      return dataMasked[count];
    }

    private SchwaemmHelper.MaskedData selectStateHigherOrder3() {
      count = (count+1) % CompleteSparkleBenchmark.ExecutionPlan.COUNT;
      return dataMasked3[count];
    }

    public static final int COUNT = 200;

    @Param({"32", "64", "128", "256", "512", "1024", "2048"})
    private int MessageLength;

    private static Random random = new Random(1234);

    private final SchwaemmHelper[] data = new SchwaemmHelper[COUNT];
    private final SchwaemmHelper.MaskedData[] dataMasked = new SchwaemmHelper.MaskedData[COUNT];
    private final SchwaemmHelper.MaskedData[] dataMasked3 = new SchwaemmHelper.MaskedData[COUNT];

    /** Setup method for benchmarks. */
    @Setup(Level.Trial)
    public void setUp() {
      for (int i = 0; i < COUNT; i++) {

        SchwaemmHelper test = SchwaemmHelper.prepareTest(SchwaemmType.S128128, MessageLength-1, MessageLength, random);

        data[i] = test;
        dataMasked[i] = SchwaemmHelper.convertDataToMasked(test, 2);
        dataMasked3[i] = SchwaemmHelper.convertDataToMasked(test, 3);
      }
    }
  }
}
