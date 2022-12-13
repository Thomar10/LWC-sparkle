package benchmarks.handin;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import schwaemm.Schwaemm;
import schwaemm.SchwaemmHelper;
import schwaemm.SchwaemmMasked;
import schwaemm.SchwaemmType;
import sparkle.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SchwaemmVariants512LengthBenchmark {


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
  public void schwaemmEncryptGoubin(ExecutionPlan plan, Blackhole blackhole) {
    SchwaemmHelper.MaskedData data = plan.selectStateFirstOrder();
    plan.goubinSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
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
    plan.booleanSchwaemm.encryptAndTag(data.message(), data.cipher(), data.associate(),
        data.key(),
        data.nonce());
    blackhole.consume(data);
  }

  /**
   * ExecutionPlan class.
   */
  @State(Scope.Benchmark)
  public static class ExecutionPlan {


    private Schwaemm schwaemmNormal;

    private SchwaemmMasked goubinSchwaemm;
    private SchwaemmMasked booleanSchwaemm;
    private SchwaemmMasked firstOrderSchwaemm;
    private SchwaemmMasked koggeStoneSchwaemm;
    private SchwaemmMasked higherOrderSchwaemm;

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

    @Param({"512"})
    private int MessageLength;

    @Param({"256128"}) //@Param({"128128", "256128" , "192192", "256256"})
    private int variant;

    private static Random random = new Random(1234);

    private final SchwaemmHelper[] data = new SchwaemmHelper[COUNT];
    private final SchwaemmHelper.MaskedData[] dataMasked = new SchwaemmHelper.MaskedData[COUNT];
    private final SchwaemmHelper.MaskedData[] dataMasked3 = new SchwaemmHelper.MaskedData[COUNT];

    /** Setup method for benchmarks. */
    @Setup(Level.Trial)
    public void setUp() {
      SchwaemmType type;

      switch (variant){
        case 128128 -> {
          type = SchwaemmType.S128128;
        }
        case 256128 -> {
          type = SchwaemmType.S256128;
        }
        case 192192 -> {
          type = SchwaemmType.S192192;
        }
        case 256256 -> {
          type = SchwaemmType.S256256;
        }
        default-> {throw new RuntimeException("Unknown Schwaemm configuration!");}
      }


      schwaemmNormal = new Schwaemm(type);
      goubinSchwaemm = new SchwaemmMasked(type, new MaskedSparkleGoubin());
      booleanSchwaemm = new SchwaemmMasked(type, new MaskedSparkleBoolean());
      firstOrderSchwaemm = new SchwaemmMasked(type, new MaskedSparkleFirstOrder());
      koggeStoneSchwaemm = new SchwaemmMasked(type, new MaskedSparkleKoggeStone());
      higherOrderSchwaemm = new SchwaemmMasked(type, new MaskedSparkleHigherOrder());


      for (int i = 0; i < COUNT; i++) {

        SchwaemmHelper test = SchwaemmHelper.prepareTest(type, MessageLength-1, MessageLength, random);

        data[i] = test;
        dataMasked[i] = SchwaemmHelper.convertDataToMasked(test, 2);
        dataMasked3[i] = SchwaemmHelper.convertDataToMasked(test, 3);
      }
    }
  }
}
