package benchmarks.handin;

import esch.Esch;
import esch.EschHelper;
import esch.EschMasked;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import sparkle.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class FullEschVariants512LengthBenchmark {
  

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void eschNormal(ExecutionPlan plan, Blackhole blackhole) {
    EschHelper data = plan.selectState();

    plan.eschNormal.crypto_hash(data.out(), data.in());

    blackhole.consume(data);
  }


  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void eschBooleanFirstOrderInput(ExecutionPlan plan, Blackhole blackhole) {
    EschHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.booleanEsch.crypto_hash(data.out(), data.in());
    blackhole.consume(data);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void eschGoubinFirstOrderInput(ExecutionPlan plan, Blackhole blackhole) {
    EschHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.goubinEsch.crypto_hash(data.out(), data.in());
    blackhole.consume(data);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void eschFirstOrder(ExecutionPlan plan, Blackhole blackhole) {
    EschHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.firstOrderEsch.crypto_hash(data.out(), data.in());
    blackhole.consume(data);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void eschKoggeStone(ExecutionPlan plan, Blackhole blackhole) {
    EschHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.koggeStoneEsch.crypto_hash(data.out(), data.in());
    blackhole.consume(data);
  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 20000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void eschHigherOrderWithFirstOrderInput(ExecutionPlan plan, Blackhole blackhole) {
    EschHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.higherOrderEsch.crypto_hash(data.out(), data.in());
    blackhole.consume(data);

  }


  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 20000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void eschHigherOrder3(ExecutionPlan plan, Blackhole blackhole) {
    EschHelper.MaskedData data = plan.selectStateHigherOrder3();
    plan.higherOrderEsch.crypto_hash(data.out(), data.in());
    blackhole.consume(data);


  }

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 10, time = 20000, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void eschBoolean3(ExecutionPlan plan, Blackhole blackhole) {
    EschHelper.MaskedData data = plan.selectStateHigherOrder3();
    plan.higherOrderEsch.crypto_hash(data.out(), data.in());
    blackhole.consume(data);
  }

  /**
   * ExecutionPlan class.
   */
  @State(Scope.Benchmark)
  public static class ExecutionPlan {

    private Esch eschNormal;
    private EschMasked booleanEsch;

    private EschMasked goubinEsch;
    private EschMasked firstOrderEsch;
    private EschMasked koggeStoneEsch;
    private EschMasked higherOrderEsch;

    int count;

    private EschHelper selectState() {
      count = (count+1) % CompleteSparkleBenchmark.ExecutionPlan.COUNT;
      return data[count];
    }

    private EschHelper.MaskedData selectStateFirstOrder() {
      count = (count+1) % CompleteSparkleBenchmark.ExecutionPlan.COUNT;
      return dataMasked[count];
    }

    private EschHelper.MaskedData selectStateHigherOrder3() {
      count = (count+1) % CompleteSparkleBenchmark.ExecutionPlan.COUNT;
      return dataMasked3[count];
    }

    public static final int COUNT = 200;

    @Param({"512"})
    private int MessageLength;

    @Param({"256", "384"})
    private int variant;

    private static Random random = new Random(1234);

    private final EschHelper[] data = new EschHelper[COUNT];
    private final EschHelper.MaskedData[] dataMasked = new EschHelper.MaskedData[COUNT];
    private final EschHelper.MaskedData[] dataMasked3 = new EschHelper.MaskedData[COUNT];

    /** Setup method for benchmarks. */
    @Setup(Level.Trial)
    public void setUp() {
      eschNormal = new Esch(variant);
      booleanEsch = new EschMasked(variant, new MaskedSparkleBoolean());

      goubinEsch = new EschMasked(variant, new MaskedSparkleGoubin());
      firstOrderEsch = new EschMasked(variant, new MaskedSparkleFirstOrder());
      koggeStoneEsch = new EschMasked(variant, new MaskedSparkleKoggeStone());
      higherOrderEsch = new EschMasked(variant, new MaskedSparkleHigherOrder());

      for (int i = 0; i < COUNT; i++) {

        EschHelper test = EschHelper.prepareTest(variant, MessageLength-1, MessageLength, random);

        data[i] = test;
        dataMasked[i] = EschHelper.convertDataToMasked(test, 2);
        dataMasked3[i] = EschHelper.convertDataToMasked(test, 3);
      }
    }
  }
}
