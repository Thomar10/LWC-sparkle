import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class MaskedSparkleBooleanTest {

  @RepeatedTest(10)
  void secondOrderMaskedSparkle256Test() {
    HigherOrderStateShares stateShares = HigherOrderStateShares.generateStateShares(2);
    Sparkle.sparkle256(stateShares.state);
    MaskedSparkleBoolean.sparkle256(stateShares.stateShares);

    int[] recoveredState = HigherOrderStateShares.recoverState(stateShares.stateShares);

    Assertions.assertThat(stateShares.state).isEqualTo(recoveredState);
  }

  @RepeatedTest(10)
  void thirdOrderMaskedSparkle256Test() {
    HigherOrderStateShares stateShares = HigherOrderStateShares.generateStateShares(3);
    Sparkle.sparkle256(stateShares.state);
    MaskedSparkleBoolean.sparkle256(stateShares.stateShares);

    int[] recoveredState = HigherOrderStateShares.recoverState(stateShares.stateShares);

    Assertions.assertThat(stateShares.state).isEqualTo(recoveredState);
  }

  @RepeatedTest(10)
  void fourthOrderMaskedSparkle256Test() {
    HigherOrderStateShares stateShares = HigherOrderStateShares.generateStateShares(4);
    Sparkle.sparkle256(stateShares.state);
    MaskedSparkleBoolean.sparkle256(stateShares.stateShares);

    int[] recoveredState = HigherOrderStateShares.recoverState(stateShares.stateShares);

    Assertions.assertThat(stateShares.state).isEqualTo(recoveredState);
  }

  @RepeatedTest(10)
  void fifthOrderMaskedSparkle256Test() {
    HigherOrderStateShares stateShares = HigherOrderStateShares.generateStateShares(5);
    Sparkle.sparkle256(stateShares.state);
    MaskedSparkleBoolean.sparkle256(stateShares.stateShares);

    int[] recoveredState = HigherOrderStateShares.recoverState(stateShares.stateShares);

    Assertions.assertThat(stateShares.state).isEqualTo(recoveredState);
  }

  @RepeatedTest(10)
  void higherOrderStateSharesTest() {
    HigherOrderStateShares stateShares = HigherOrderStateShares.generateStateShares(5);

    Assertions.assertThat(stateShares.state).isNotEqualTo(stateShares.stateShares[0]);

    int[] recoveredState = HigherOrderStateShares.recoverState(stateShares.stateShares);

    Assertions.assertThat(stateShares.state).isEqualTo(recoveredState);
  }

  record HigherOrderStateShares(int[] state, int[][] stateShares) {

    static HigherOrderStateShares generateStateShares(int order) {
      int shareLength = Sparkle.maxBranches * 2;

      int[] state = generateRandomShare(shareLength);
      int[][] stateShares = new int[order][shareLength];

      System.arraycopy(state, 0, stateShares[0], 0, shareLength);

      for (int i = 1; i < order; i++) {
        stateShares[i] = generateRandomShare(shareLength);
        maskShare(stateShares[0], stateShares[i]);
      }

      return new HigherOrderStateShares(state, stateShares);
    }

    static void maskShare(int[] share, int[] mask) {
      for (int i = 0; i < share.length; i++) {
        share[i] ^= mask[i];
      }
    }

    static int[] generateRandomShare(int length) {
      Random random = new Random();

      int[] share = new int[length];

      for (int i = 0; i < length; i++) {
        share[i] = random.nextInt(Integer.MAX_VALUE);
      }

      return share;
    }

    static int[] recoverState(int[][] stateShares) {
      int[] recoveredState = stateShares[0];

      for (int i = 1; i < stateShares.length; i++) {
        maskShare(recoveredState, stateShares[i]);
      }

      return recoveredState;
    }
  }
}
