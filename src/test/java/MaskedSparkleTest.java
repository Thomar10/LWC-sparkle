import java.util.Arrays;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class MaskedSparkleTest {

  static Random random = new Random(123);

  static int[][] generateRandomMaskedState(int[] state) {
    Random random = new Random();

    int[] share0 = new int[state.length];
    int[] share1 = new int[state.length];
    for (int i = 0; i < share1.length; i++) {
      int randomNumber = random.nextInt(Integer.MAX_VALUE);
      share0[i] = randomNumber;
      share1[i] = state[i] ^ randomNumber;
    }
    return new int[][]{share0, share1};
  }

  static int[] recoverState(int[][] state) {
    int[] result = new int[state[0].length];
    for (int i = 0; i < state[0].length; i++) {
      result[i] = state[0][i] ^ state[1][i];
    }
    return result;
  }

  @Test
  void maskedSparkle() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedState(states.copy);
    SparkleMasked.sparkle256(state);
    Sparkle.sparkle256(states.stateNormal);
    Assertions.assertThat(states.stateNormal).isEqualTo(recoverState(state));
  }

  @RepeatedTest(10)
  void generateAndRecover() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedState(states.copy);
    Assertions.assertThat(states.stateNormal).isEqualTo(recoverState(state));
  }

  record RandomMaskedState(int[] stateNormal, int[] copy) {

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
