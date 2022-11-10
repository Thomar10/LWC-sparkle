import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

/**
 * Test java implementation with C implementation of sparkle.
 */
public final class SparkleTest {

  @RepeatedTest(10)
  void sparkle256Test()  {
    RandomState states = RandomState.generateRandomState();
    SparkleLib.sparkleC(states.stateC, 4, 10);
    Sparkle.sparkle256(states.stateJava);
    Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
  }

  @RepeatedTest(10)
  void sparkle256SlimTest() {
    RandomState states = RandomState.generateRandomState();
    SparkleLib.sparkleC(states.stateC, 4, 7);
    Sparkle.sparkle256Slim(states.stateJava);
    Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
  }

  @RepeatedTest(10)
  void sparkle384Test() {
    RandomState states = RandomState.generateRandomState();
    SparkleLib.sparkleC(states.stateC, 6, 11);
    Sparkle.sparkle384(states.stateJava);
    Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
  }

  @RepeatedTest(10)
  void sparkle384SlimTest() {
    RandomState states = RandomState.generateRandomState();
    SparkleLib.sparkleC(states.stateC, 6, 7);
    Sparkle.sparkle384Slim(states.stateJava);
    Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
  }

  @RepeatedTest(10)
  void sparkle512Test() {
    RandomState states = RandomState.generateRandomState();
    SparkleLib.sparkleC(states.stateC, 8, 12);
    Sparkle.sparkle512(states.stateJava);
    Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
  }

  @RepeatedTest(10)
  void sparkle512SlimTest() {
    RandomState states = RandomState.generateRandomState();
    SparkleLib.sparkleC(states.stateC, 8, 8);
    Sparkle.sparkle512Slim(states.stateJava);
    Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
  }

  record RandomState(int[] stateC, int[] stateJava) {
    static RandomState generateRandomState() {
      Random random = new Random();
      int[] stateC = new int[Sparkle.maxBranches * 2];
      int[] stateJ = new int[Sparkle.maxBranches * 2];
      for (int i = 0; i < stateC.length; i++) {
        int randomNumber = random.nextInt(Integer.MAX_VALUE);
        stateC[i] = randomNumber;
        stateJ[i] = randomNumber;
      }
      return new RandomState(stateC, stateJ);
    }
  }
}
