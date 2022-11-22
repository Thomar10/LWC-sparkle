import java.util.Random;

/**
 * Sparkle permutation.
 */
public final class SparkleMaskedSingleThread {
  static Random random = new Random();
  public static final int maxBranches = 8;
  private static final int[] rcon =
      new int[]{
          -1209970334,
          -1083090816,
          951376470,
          844003128,
          -1156479509,
          1333558103,
          -809524792,
          -1028445891
      };

  public static void sparkle256(int[] state) {
    runSparkle(state, 4, 10);
  }

  public static void sparkle256Slim(int[] state) {
    runSparkle(state, 4, 7);
  }

  public static void sparkle384(int[] state) {
    runSparkle(state, 6, 11);
  }

  public static void sparkle384Slim(int[] state) {
    runSparkle(state, 6, 7);
  }

  public static void sparkle512(int[] state) {
    runSparkle(state, 8, 12);
  }

  public static void sparkle512Slim(int[] state) {
    runSparkle(state, 8, 8);
  }

  public static void runSparkle(int[] state, int brans, int steps) {
    int[] share0 = new int[state.length];
    int[] share1 = new int[state.length];

    generateRandomMaskedState(state, share0, share1);

    sparkle(share0, share1, brans, steps);

    recoverState(state, share0, share1);
  }

  static void generateRandomMaskedState(int[] state, int[] share0, int[] share1) {
    Random random = new Random();

    for (int i = 0; i < share1.length; i++) {
      int randomNumber = random.nextInt(Integer.MAX_VALUE);

      share0[i] = randomNumber;
      share1[i] = state[i] ^ randomNumber;

    }
  }

  static void recoverState(int[] state, int[] share0, int[] share1) {
    for (int i = 0; i < state.length; i++) {
      state[i] = share0[i] ^ share1[i];
    }
  }

  static int rot(int x, int n) {
    return (x >>> n) | (x << (32 - n));
  }

  static int ell(int x) {
    return rot(x ^ (x << 16), 16);
  }

  private static void sparkle(int[] share0, int[] share1, int brans, int steps) {
    int rc, tmpx, tmpy, x0, y0;
    for (int i = 0; i < steps; i++) {
      share0[1] ^= rcon[i % maxBranches];
      share0[3] ^= i;

      for (int j = 0; j < 2 * brans; j += 2) {
        rc = rcon[j >> 1];
        alzetteRound(share0, share1, j, 31, 24, rc);
        alzetteRound(share0, share1, j, 17, 17, rc);
        alzetteRound(share0, share1, j, 0, 31, rc);
        alzetteRound(share0, share1, j, 24, 16, rc);
      }
      BinarySparkleOperations(share0, brans);
      BinarySparkleOperations(share1, brans);
    }
  }

  private static void BinarySparkleOperations(int[] state, int brans) {
    int y0;
    int x0;
    int tmpy;
    int tmpx;
    tmpx = x0 = state[0];
    tmpy = y0 = state[1];
    for (int j = 2; j < brans; j += 2) {
      tmpx ^= state[j];
      tmpy ^= state[j + 1];
    }
    tmpx = ell(tmpx);
    tmpy = ell(tmpy);
    for (int j = 2; j < brans; j += 2) {
      state[j - 2] = state[j + brans] ^ state[j] ^ tmpy;
      state[j + brans] = state[j];
      state[j - 1] = state[j + brans + 1] ^ state[j + 1] ^ tmpx;
      state[j + brans + 1] = state[j + 1];
    }
    state[brans - 2] = state[brans] ^ x0 ^ tmpy;
    state[brans] = x0;
    state[brans - 1] = state[brans + 1] ^ y0 ^ tmpx;
    state[brans + 1] = y0;
  }

  static void alzetteRound(int[] share0, int[] share1, int j, int shiftOne, int shiftTwo, int rc) {
    // Let state[j] be x and state[j+1] be y
    //state[j] += rot(state[j + 1], shiftOne);

    int toAddShare0 = rot(share0[j + 1], shiftOne);
    int toAddShare1 = rot(share1[j + 1], shiftOne);

    addArithmeticToBinary(share0, toAddShare0, toAddShare1, share1[j], j);

    share0[j + 1] ^= rot(share0[j], shiftTwo);
    share0[j] ^= rc;

    share1[j + 1] ^= rot(share1[j], shiftTwo);
  }

  static void addArithmeticToBinary(int[] state, int toAdd, int otherToAdd, int otherShares, int j) {
    //Convert to arithmetic
    state[j] = binaryToArithmetic(state[j], otherShares);
    int toAddArith = binaryToArithmetic(toAdd, otherToAdd);

    state[j] += toAddArith + otherToAdd;

    //Convert to binary
    state[j] = arithmeticToBinary(state[j], otherShares);
  }

  static int binaryToArithmetic(int x, int r) {
    int gamma = random.nextInt(Integer.MAX_VALUE);
    int T = x ^ gamma;
    T = T - gamma;
    T = T ^ x;
    gamma = gamma ^ r;
    int A = x ^ gamma;
    A = A - gamma;
    return A ^ T;
  }

  static int arithmeticToBinary(int A, int r) {
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
}
