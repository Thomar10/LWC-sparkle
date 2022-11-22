import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public final class SparkleMaskedSingleThreadTest {
    @RepeatedTest(10)
    void sparkle256Test()  {
        SparkleMaskedSingleThreadTest.RandomState states = SparkleMaskedSingleThreadTest.RandomState.generateRandomState();
        SparkleLib.sparkleC(states.stateC, 4, 10);
        SparkleMaskedSingleThread.sparkle256(states.stateJava);
        Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
    }

    @RepeatedTest(10)
    void sparkle256SlimTest() {
        SparkleMaskedSingleThreadTest.RandomState states = SparkleMaskedSingleThreadTest.RandomState.generateRandomState();
        SparkleLib.sparkleC(states.stateC, 4, 7);
        SparkleMaskedSingleThread.sparkle256Slim(states.stateJava);
        Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
    }

    @RepeatedTest(10)
    void sparkle384Test() {
        SparkleMaskedSingleThreadTest.RandomState states = SparkleMaskedSingleThreadTest.RandomState.generateRandomState();
        SparkleLib.sparkleC(states.stateC, 6, 11);
        SparkleMaskedSingleThread.sparkle384(states.stateJava);
        Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
    }

    @RepeatedTest(10)
    void sparkle384SlimTest() {
        SparkleMaskedSingleThreadTest.RandomState states = SparkleMaskedSingleThreadTest.RandomState.generateRandomState();
        SparkleLib.sparkleC(states.stateC, 6, 7);
        SparkleMaskedSingleThread.sparkle384Slim(states.stateJava);
        Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
    }

    @RepeatedTest(10)
    void sparkle512Test() {
        SparkleMaskedSingleThreadTest.RandomState states = SparkleMaskedSingleThreadTest.RandomState.generateRandomState();
        SparkleLib.sparkleC(states.stateC, 8, 12);
        SparkleMaskedSingleThread.sparkle512(states.stateJava);
        Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
    }

    @RepeatedTest(10)
    void sparkle512SlimTest() {
        SparkleMaskedSingleThreadTest.RandomState states = SparkleMaskedSingleThreadTest.RandomState.generateRandomState();
        SparkleLib.sparkleC(states.stateC, 8, 8);
        SparkleMaskedSingleThread.sparkle512Slim(states.stateJava);
        Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
    }

    record RandomState(int[] stateC, int[] stateJava) {
        static SparkleMaskedSingleThreadTest.RandomState generateRandomState() {
            Random random = new Random();
            int[] stateC = new int[Sparkle.maxBranches * 2];
            int[] stateJ = new int[Sparkle.maxBranches * 2];
            for (int i = 0; i < stateC.length; i++) {
                int randomNumber = random.nextInt(Integer.MAX_VALUE);
                stateC[i] = randomNumber;
                stateJ[i] = randomNumber;
            }
            return new SparkleMaskedSingleThreadTest.RandomState(stateC, stateJ);
        }
    }
}
