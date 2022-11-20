import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class ThreadedMaskedSparkle {

  private static int[] share0;
  private static int[] share1;

  private ConcurrentHashMap<String, int[]> shares;

  public void sparkle256(int[] state) throws InterruptedException {
    generateRandomMaskedState(state);
    shares = new ConcurrentHashMap<>();

    Thread one = new Thread(() -> new SparkleCopy(share0, shares).sparkle256());
    Thread two = new Thread(() -> new MaskedSparkle(share1, shares).sparkle256());
    one.start();
    two.start();
    Thread.sleep(10);
    one.join();
    two.join();
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
