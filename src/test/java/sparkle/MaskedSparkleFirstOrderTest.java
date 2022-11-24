package sparkle;

import java.util.Arrays;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class MaskedSparkleFirstOrderTest {

  static Random random = new Random();

  static int[][] generateRandomMaskedState(int[] state) {
    return generateRandomMaskedState(state, 2);
  }

  static int[][] generateRandomMaskedStateArithmetic(int[] state, int order) {
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
        resultMask -= maskedState[i][j];
      }
      maskedState[0][j] = resultMask;
    }

    return maskedState;
  }

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

  static int[] recoverStateArithmetic(int[][] state) {
    int[] result = new int[state[0].length];
    for (int i = 0; i < state[0].length; i++) {
      int resultMask = state[0][i];
      for (int j = 1; j < state.length; j++) {
        resultMask += state[j][i];
      }
      result[i] = resultMask;
    }
    return result;
  }

  @RepeatedTest(50)
  void maskedSparkle256() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedState(states.copy);
    MaskedSparkleFirstOrder.sparkle256(state);
    Sparkle.sparkle256(states.stateNormal);
    Assertions.assertThat(states.stateNormal).isEqualTo(recoverState(state));
  }

  @RepeatedTest(50)
  void maskedSparkleSlim256() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedState(states.copy);
    MaskedSparkleFirstOrder.sparkle256Slim(state);
    Sparkle.sparkle256Slim(states.stateNormal);
    Assertions.assertThat(states.stateNormal).isEqualTo(recoverState(state));
  }

  @RepeatedTest(50)
  void maskedSparkle256HigherOrder() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedState(states.copy, 3);
    MaskedSparkleFirstOrder.sparkle256(state);
    Sparkle.sparkle256(states.stateNormal);
    Assertions.assertThat(states.stateNormal).isEqualTo(recoverState(state));
  }

  @RepeatedTest(1)
  void convertBackAndFourth() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedState(states.copy, 3);
    System.out.println(Arrays.toString(states.stateNormal));
    int[] converted = MaskedSparkleHigherOrder.booleanToArithmeticHigherOrder(state, 0);
    int result = 0;
    for (int number : converted) {
      result += number;
    }
    Assertions.assertThat(states.stateNormal[0]).isEqualTo(result);
  }

  @RepeatedTest(50)
  void sparkleOrder4() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedStateArithmetic(states.copy, 4);
    MaskedSparkleFirstOrder.sparkle256(state);
    Sparkle.sparkle256(states.stateNormal);
    Assertions.assertThat(recoverState(state)).isEqualTo(states.stateNormal);
  }

  @RepeatedTest(50)
  void convertBackAndFourthArithmetic() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedStateArithmetic(states.copy, 4);
    int[] toConvert = new int[]{state[0][0], state[1][0], state[2][0], state[3][0]};
    int[] converted = MaskedSparkleHigherOrder.convertAToB(toConvert);
    Assertions.assertThat(states.stateNormal[0])
        .isEqualTo(converted[0] ^ converted[1] ^ converted[2] ^ converted[3]);
  }

  @RepeatedTest(1)
  void convertBackAndFourthArithmetic2() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedStateArithmetic(states.copy, 3);
    int[] toConvert = new int[]{state[0][0], state[1][0], state[2][0]};
    System.out.println("toConvert " + Arrays.toString(toConvert));
    System.out.println("States normal " + states.stateNormal[0]);
    System.out.println("Reconstructed " + (state[0][0] + state[1][0] + state[2][0]));
    System.out.println("The first two "  + (state[0][0] + state[1][0]));
    int[] converted = MaskedSparkleHigherOrder.convertAToB(toConvert);
    Assertions.assertThat(states.stateNormal[0])
        .isEqualTo(converted[0] ^ converted[1] ^ converted[2]);
  }

  @RepeatedTest(1)
  void convertBackAndFourthArithmetic5() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedStateArithmetic(states.copy, 5);
    int[] toConvert = new int[]{state[0][0], state[1][0], state[2][0], state[3][0], state[4][0]};
    int[] converted = MaskedSparkleHigherOrder.convertAToB(toConvert);
    Assertions.assertThat(states.stateNormal[0])
        .isEqualTo(converted[0] ^ converted[1] ^ converted[2]);
  }

  @RepeatedTest(10)
  void generateAndRecoverArithmetic() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[][] state = generateRandomMaskedStateArithmetic(states.copy, 2);
    Assertions.assertThat(states.stateNormal).isEqualTo(recoverStateArithmetic(state));
  }

  @RepeatedTest(10)
  void expandTest() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[] expanded = MaskedSparkleHigherOrder.expand(states.stateNormal);
    int expandedSum = 0;
    for (int number : expanded) {
      expandedSum ^= number;
    }
    int sum = 0;
    for (int number : states.stateNormal) {
      expandedSum ^= number;
    }
    Assertions.assertThat(expandedSum).isEqualTo(sum);
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
