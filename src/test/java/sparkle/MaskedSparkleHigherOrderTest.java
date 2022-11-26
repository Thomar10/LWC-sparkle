package sparkle;

import java.util.Arrays;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class MaskedSparkleHigherOrderTest {

  static Random random = new Random();

  static int[][] generateRandomMaskedState(int[] state, int order) {
    int[][] maskedState = new int[order][state.length];
    for (int i = 1; i < order; i++) {
      for (int j = 0; j < state.length; j++) {
        int number = random.nextInt(Integer.MAX_VALUE);
        maskedState[i][j] = number;
      }
    }
    for (int j = 0; j < state.length; j++) {
      int resultMask = state[j];
      for (int i = 1; i < order; i++) {
        resultMask ^= maskedState[i][j];
      }
      maskedState[0][j] = resultMask;
    }

    return maskedState;
  }

  static int[] recoverState(int[][] state) {
    int[] result = new int[state[0].length];
    for (int i = 0; i < state[0].length; i++) {
      int resultMask = state[0][i];
      for (int j = 1; j < state.length; j++) {
        resultMask ^= state[j][i];
      }
      result[i] = resultMask;
    }
    return result;
  }

  @RepeatedTest(50)
  void maskedSparkle256() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedState(states.copy, 4);
    MaskedSparkleHigherOrder.sparkle256(state);
    Sparkle.sparkle256(states.stateNormal);
    Assertions.assertThat(states.stateNormal).isEqualTo(recoverState(state));
  }

  @RepeatedTest(1)
  void maskedSparkleSlim256() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedState(states.copy, 4);
    int[][] state2 = new int [state.length][];
    for (int i = 0; i < state.length; i++) {
      state2[i] = Arrays.copyOf(state[i], state[i].length);
    }
    MaskedSparkleHigherOrder.sparkle256Slim(state);
    MaskedSparkleBoolean.sparkle256Slim(state2);
    System.out.println(Arrays.toString(state[0]));
    System.out.println(Arrays.toString(state2[0]));
    Sparkle.sparkle256Slim(states.stateNormal);
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
