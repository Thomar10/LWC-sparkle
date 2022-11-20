import java.util.Random;

public final class ThreadedMaskedSparkle {

  private static int[] share0;
  private static int[] share1;

  public static void sparkle256(int[] state) {
    generateRandomMaskedState(state);
    Thread one = new Thread(() -> MaskedSparkle.sparkle256(share0));
    Thread two = new Thread(() -> PassiveSparkle.sparkle256(share1));
    MaskedSparkle.sparkle256(share0);
    PassiveSparkle.sparkle256(share1);
    recoverState(state);
  }

  static void generateRandomMaskedState(int[] state) {
    Random random = new Random();

    share0 = new int[state.length];
    share1 = new int[state.length];

    for (int i = 0; i < share1.length; i++) {
      int randomNumber = random.nextInt(Integer.MAX_VALUE);

      share0[i] = randomNumber;
      share1[i] = state[i] ^ randomNumber;

    }

  }

  static void recoverState(int[] state) {
    for (int i = 0; i < state.length; i++) {
      state[i] = share0[i] ^ share1[i];
    }
  }


}
