import java.util.Arrays;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;


public class ABOXTest {

  static Random random = new Random(123);


  @Test
  public void conversion() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[] share0 = generateRandomMaskedState(states.copy());
    System.out.println("Start:   " + states.stateNormal[0]);
    System.out.println("Start:   " + (share0[0] ^ states.copy[0]));
    int shareCopy = binaryToArithmetic(states.copy[0], share0[0]);
    int toAdd = binaryToArithmetic(states.copy[1], share0[1]);
    System.out.println("toAdd: " + toAdd);
    System.out.println("other: " + (states.copy[1] ^ share0[1]));
    System.out.println("toAdd: " + (toAdd + share0[1]));
    System.out.println("Answer " + states.stateNormal[1]);
    shareCopy += toAdd + share0[1];
    //shareCopy += (states.copy[1] ^ share0[1]);
    System.out.println("Arithmetic:       " + (shareCopy + share0[0]));
    states.stateNormal[0] += states.stateNormal[1];

    int shareCopyBack = arithmeticToBinary(shareCopy, (share0[0]));
    System.out.println("Boolean:          " + (share0[0] ^ shareCopyBack));
    System.out.println("Answer after add: " + states.stateNormal[0]);
  }

  @Test
  public void abox() {
    RandomMaskedState states = RandomMaskedState.generateRandomMaskedState();
    int[] share0 = generateRandomMaskedState(states.copy());

    //System.out.println(Arrays.toString(alzetteRoundSparkle(states.stateNormal, 0, 31)));
    alzetteRoundSparkle(states.stateNormal, 0, 31);
    //System.out.println(Arrays.toString(alzetteRoundCopy(share0, 0, 31)));
    alzetteRoundCopy(share0, 0, 31);
    //System.out.println(Arrays.toString(alzetteRoundMasked(states.copy, 0, 31)));
    alzetteRoundMasked(states.copy, 0, 31);
    Assertions.assertThat(share0[0] ^ states.copy[0]).isEqualTo(states.stateNormal[0]);

  }

  public static int binaryToArithmetic(int x, int r) {
    long gamma = random.nextInt(Integer.MAX_VALUE);
    long T = x ^ gamma;
    T = T - gamma;
    T = T ^ x;
    gamma = gamma ^ r;
    long A = x ^ gamma;
    A = A - gamma;
    return (int) (A ^ T);
  }

  public static int arithmeticToBinary(int A, int r) {
    int gamma = random.nextInt(Integer.MAX_VALUE);
    int T = 2 * gamma;
    int x = gamma ^ r;
    int omega = gamma & x;
    x = T ^ A;
    gamma = gamma ^ x;
    gamma = gamma & r;
    omega = omega ^ gamma;
    gamma = T & A;
    omega = omega ^ gamma;
    for (int k = 1; k < 32; k++) {
      gamma = T & r;
      gamma = gamma ^ omega;
      T = T & A;
      gamma = gamma ^ T;
      T = 2 * gamma;
    }
    return x ^ T;
  }

  static int[] alzetteRoundSparkle(int[] state, int j, int shiftOne) {
    // Let state[j] be x and state[j+1] be y
    int toAdd = rot(state[j + 1], shiftOne);
    System.out.println("Sparkle to add: " + toAdd);
    System.out.println("Sparkle state J: " + state[j]);
    state[j] = state[j] + toAdd;
    System.out.println("Sparkle state J: " + state[j]);
    return state;
  }

  static int[] alzetteRoundCopy(int[] state, int j, int shiftOne) {
    // Let state[j] be x and state[j+1] be y
    int[] share = new int[]{574963334, 952629791};
    //state[j] += rot(state[j + 1], shiftOne);
    return state;
  }


  static int[] alzetteRoundMasked(int[] state, int j, int shiftOne) {
    // Let state[j] be x and state[j+1] be y
    int[] theOtherShare = new int[]{2127939176, 647624789};
    int shareRot = rot(theOtherShare[j+1], shiftOne);
    int toAdd = binaryToArithmetic(rot(state[j + 1], shiftOne), shareRot);
    System.out.println("Masked toAdd " + (toAdd + shareRot));
    int stateJ = binaryToArithmetic(state[j], theOtherShare[j]);
    System.out.println("Masked state[j] " + (stateJ + theOtherShare[j]));
    state[j] =  stateJ + toAdd + shareRot;

    state[j] = arithmeticToBinary(state[j], theOtherShare[j]);
    System.out.println("Masked state[j] " + (state[j] ^ theOtherShare[j]));
    return state;
  }

  static int rot(int x, int n) {
    return (x >>> n) | (x << (32 - n));
  }

  static int[] generateRandomMaskedState(int[] state) {

    int[] share0 = new int[state.length];

    for (int i = 0; i < share0.length; i++) {
      int randomNumber = random.nextInt(Integer.MAX_VALUE);

      share0[i] = randomNumber;
      state[i] = state[i] ^ randomNumber;
    }
    return share0;
  }

  static void recoverState(int[] state, int[] share0) {
    for (int i = 0; i < state.length; i++) {
      state[i] = share0[i] ^ state[i];
    }
  }

  record RandomMaskedState(int[] stateNormal, int[] copy) {

    static RandomMaskedState generateRandomMaskedState() {

      int[] stateN = new int[2];

      for (int i = 0; i < stateN.length; i++) {
        int randomNumber = random.nextInt(Integer.MAX_VALUE);
        stateN[i] = randomNumber;
      }

      return new RandomMaskedState(stateN, Arrays.copyOf(stateN, stateN.length));
    }
  }
}
