package sparkle;

import java.util.Random;

/**
 * sparkle.Sparkle permutation.
 */
public final class MaskedSparkleFirstOrder {

  public static final int maxBranches = 8;
  private static final int[] rcon = new int[]{-1209970334, -1083090816, 951376470, 844003128,
      -1156479509, 1333558103, -809524792, -1028445891};
  private static final Random random = new Random(123);

  public static void sparkle256(int[][] state) {
    sparkle(state, 4, 10);
  }

  public static void sparkle256Slim(int[][] state) {
    sparkle(state, 4, 7);
  }

  public static void sparkle384(int[][] state) {
    sparkle(state, 6, 11);
  }

  public static void sparkle384Slim(int[][] state) {
    sparkle(state, 6, 7);
  }

  public static void sparkle512(int[][] state) {
    sparkle(state, 8, 12);
  }

  public static void sparkle512Slim(int[][] state) {
    sparkle(state, 8, 8);
  }

  static int rot(int x, int n) {
    return (x >>> n) | (x << (32 - n));
  }

  static int ell(int x) {
    return rot(x ^ (x << 16), 16);
  }

  private static void sparkle(int[][] state, int brans, int steps) {
    int toAdd;
    for (int i = 0; i < steps; i++) {
      state[0][1] ^= rcon[i % maxBranches];
      state[0][3] ^= i;
      for (int k = 0; k < state.length - 1; k++) {
        for (int j = 0; j < 2 * brans; j += 2) {
          int rc = rcon[j >> 1];
          toAdd = alzetteRound(state, j, 31, 24, k);
          alzetteRoundLast(state, j, 31, 24, rc, toAdd);

          toAdd = alzetteRound(state, j, 17, 17, k);
          alzetteRoundLast(state, j, 17, 17, rc, toAdd);

          toAdd = alzetteRound(state, j, 0, 31, k);
          alzetteRoundLast(state, j, 0, 31, rc, toAdd);

          toAdd = alzetteRound(state, j, 24, 16, k);
          alzetteRoundLast(state, j, 24, 16, rc, toAdd);
        }
      }
      for (int[] s : state) {
        binarySparkleOperations(s, brans);
      }
    }
  }

  static void binarySparkleOperations(int[] state, int brans) {
    int tmpx, tmpy, x0, y0;
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

  static int alzetteRound(int[][] state, int j, int shiftOne, int shiftTwo, int index) {
    int toAdd = rot(state[index][j + 1], shiftOne);
    int stateJ = binaryToArithmetic(state[index][j], state[1][j]);
    stateJ += toAdd;
    for (int i = 0; i < state.length - 1; i++) {
      if (i == j) {
        continue;
      }
      // TODO HMM for higher order xd
    }
    state[index][j] = arithmeticToBinary(stateJ, state[1][j]);
    state[index][j + 1] ^= rot(state[index][j], shiftTwo);
    return toAdd;
  }

  // TODO FIX FOR HIGHER ORDER!
  static void alzetteRoundLast(int[][] state, int j, int shiftOne, int shiftTwo, int rc,
      int toAddOther) {
    int index = state.length - 1;
    int toAdd = rot(state[index][j + 1], shiftOne);
    int stateJ = binaryToArithmetic(state[index][j], state[0][j]);
    int toAddArith = binaryToArithmetic(toAdd, toAddOther);
    state[index][j] = arithmeticToBinary(stateJ + toAddArith, state[0][j]);
    state[index][j + 1] ^= rot(state[1][j], shiftTwo);
    state[index][j] ^= rc;
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
}