import java.util.Arrays;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class MaskedSparkleTest {

  static Random random = new Random();

  @RepeatedTest(1)
  void maskedSparkle256Test() throws InterruptedException {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    new ThreadedMaskedSparkle().sparkle256(states.stateNormal2);
    Sparkle.sparkle256(states.stateNormal);
    System.out.println(Arrays.toString(states.stateNormal2));
    Assertions.assertThat(states.stateNormal).isEqualTo(states.stateNormal2);
  }


  record RandomMaskedState(int[] stateNormal, int[] stateNormal2) {

    static RandomMaskedState generateRandomMaskedState() {

      int[] stateN = new int[Sparkle.maxBranches * 2];

      for (int i = 0; i < stateN.length; i++) {
        int randomNumber = random.nextInt(Integer.MAX_VALUE);
        stateN[i] = randomNumber;
      }

      return new RandomMaskedState(stateN, Arrays.copyOf(stateN, stateN.length));
    }
  }
}
