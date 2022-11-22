import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public final class SparkleMaskedTest3 {
    @RepeatedTest(10)
    void sparkle256Test()  {
        SparkleMaskedTest3.RandomState states = SparkleMaskedTest3.RandomState.generateRandomState3();
        SparkleLib.sparkleC(states.stateC, 4, 10);
        SparkleMaskedNoThread.sparkle256(states.stateJava);
        Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
    }

    @Test
    void sparkle256()  {
        SparkleMaskedTest3.RandomState states = SparkleMaskedTest3.RandomState.generateRandomState3();
        SparkleNoAlzeteTest.sparkle256(states.stateC);
        SparkleMaskedNoThread.sparkle256(states.stateJava);
        Assertions.assertThat(states.stateC).isEqualTo(states.stateJava);
    }

    record RandomState(int[] stateC, int[] stateJava) {
        static SparkleMaskedTest3.RandomState generateRandomState3() {
            Random random = new Random();
            int[] stateC = new int[Sparkle.maxBranches * 2];
            int[] stateJ = new int[Sparkle.maxBranches * 2];
            for (int i = 0; i < stateC.length; i++) {
                int randomNumber = random.nextInt(Integer.MAX_VALUE);
                stateC[i] = randomNumber;
                stateJ[i] = randomNumber;
            }
            return new SparkleMaskedTest3.RandomState(stateC, stateJ);
        }
    }
}
