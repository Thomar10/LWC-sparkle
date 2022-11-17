import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Random;

public class MaskedSparkleTest {

    @RepeatedTest(10)
    void maskedSparkle256Test(){
        RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
        Sparkle.sparkle256(states.stateNormal);
        MaskedSparkle.sparkle256(states.stateMasked);
        MaskedSparkle.sparkle256(states.mask);

        recoverState(states.stateMasked, states.mask);

        Assertions.assertThat(states.stateNormal).isEqualTo(states.stateMasked);
    }

    /**
     * Recovers normal state from masked state + mask
     */
    void recoverState(int[] maskedState, int[] mask){
        for(int i = 0; i < maskedState.length; i++){
            maskedState[i] = maskedState[i] ^ mask[i];
        }
    }

    /***
     * Creates a very simple mask where stateMasked = stateNormal XOR mask
     * @param stateNormal normal state to be run through unmasked sparkle
     * @param stateMasked masked state to be run through masked sparkle each entry stateMasked[i] = stateNormal[i] ^ mask[i]
     * @param mask mask used to create stateMasked from stateNormal. Should also be run through masked sparkle
     */
    record RandomMaskedState(int[] stateNormal, int[] stateMasked, int[] mask){
        static RandomMaskedState generateRandomMaskedState(){
            Random random = new Random();

            int[] stateN = new int[Sparkle.maxBranches * 2];
            int[] stateM = new int[Sparkle.maxBranches * 2];
            int[] mask = new int[Sparkle.maxBranches * 2];

            for(int i = 0; i < stateN.length; i++){
                int randomNumber = random.nextInt(Integer.MAX_VALUE);
                int randomMask = random.nextInt(Integer.MAX_VALUE);

                stateN[i] = randomNumber;
                stateM[i] = randomNumber ^ randomMask;
                mask[i] = randomMask;
            }

            return new RandomMaskedState(stateN, stateM, mask);
        }
    }
}
