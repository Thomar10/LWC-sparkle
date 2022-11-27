package sparkle;

import java.util.Arrays;
import java.util.Random;

/**
 * sparkle.Sparkle permutation.
 */
public final class MaskedSparkleHigherOrder implements MaskedSparkle {

  public static final int maxBranches = 8;
  private static final int[] rcon = new int[]{-1209970334, -1083090816, 951376470, 844003128,
      -1156479509, 1333558103, -809524792, -1028445891};
  private static final Random random = new Random(123);

  static int rot(int x, int n) {
    return (x >>> n) | (x << (32 - n));
  }

  static int ell(int x) {
    return rot(x ^ (x << 16), 16);
  }

  private static void sparkle(int[][] state, int brans, int steps) {
    for (int i = 0; i < steps; i++) {
      state[0][1] ^= rcon[i % maxBranches];
      state[0][3] ^= i;
      for (int j = 0; j < 2 * brans; j += 2) {
        int rc = rcon[j >> 1];
        alzetteRound(state, j, 31, 24, rc);
        alzetteRound(state, j, 17, 17, rc);
        alzetteRound(state, j, 0, 31, rc);
        alzetteRound(state, j, 24, 16, rc);
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

  static void alzetteRound(int[][] state, int j, int shiftOne, int shiftTwo, int rc) {
    int[] toAdds = new int[state.length];
    int[] statesJ = new int[state.length];
    for (int i = 0; i < toAdds.length; i++) {
      toAdds[i] = rot(state[i][j + 1], shiftOne);
      statesJ[i] = state[i][j];
    }

    int[] statesA = booleanToArithmeticHigherOrder(statesJ);
    int[] toAddsA = booleanToArithmeticHigherOrder(toAdds);
    for (int i = 0; i < state.length; i++) {
      statesA[i] = statesA[i] + toAddsA[i];
    }
    int[] statesB = convertAToB(statesA);
    for (int i = 0; i < state.length; i++) {
      state[i][j] = statesB[i];
      state[i][j + 1] ^= rot(state[i][j], shiftTwo);
    }
    state[0][j] ^= rc; //Only XOR by constant to first share or every odd share
  }

  static void alzetteRoundLast(int[][] state, int j, int shiftOne, int shiftTwo, int rc,
      int toAddOther) {
    if (state.length == 2) {
      int index = state.length - 1;
      int toAdd = rot(state[index][j + 1], shiftOne);
      int stateJ = binaryToArithmetic(state[index][j], state[0][j]);
      int toAddArith = binaryToArithmetic(toAdd, toAddOther);
      state[index][j] = arithmeticToBinary(stateJ + toAddArith, state[0][j]);
      state[index][j + 1] ^= rot(state[1][j], shiftTwo);
      state[index][j] ^= rc;
    } else {

    }
  }

  public static int[] booleanToArithmeticHigherOrder(int x[]) {

    int[] A = new int[x.length];
    int[] ADot = new int[x.length];
    for (int i = 0; i < A.length - 1; i++) {
      int rand = random.nextInt(Integer.MAX_VALUE);
      A[i] = rand;
      ADot[i] = -rand;
    }

    int[] y = convertAToB(ADot);
    int[] z = BooleanAddition.secureBooleanAdditionGoubin(x, y);
    A[A.length - 1] = fullXOR(z);
    return A;
  }

  private static int fullXOR(int[] y) {
    refreshMasks(y);
    int res = 0;
    for (int number : y) {
      res ^= number;
    }
    return res;
  }

  private static void refreshMasks(int[] toRefresh) {
    for (int j = 1; j < toRefresh.length; j++) {
      int tmp = random.nextInt(Integer.MAX_VALUE);
      toRefresh[0] ^= tmp;
      toRefresh[j] ^= tmp;
    }
  }

  public static int[] convertAToBEven(int[] A) {
    if (A.length == 1) {
      return A;
    }

    int[] aLower = Arrays.copyOf(A, A.length / 2);
    int[] x = convertAToBEven(aLower);
    int[] xDot = expand(x);
    int[] aUpper = Arrays.copyOfRange(A, A.length / 2, A.length);
    int[] y = convertAToBEven(aUpper);
    int[] yDot = expand(y);

    return BooleanAddition.secureBooleanAdditionGoubin(xDot, yDot);
  }

  public static int[] convertAToBOdd(int[] A) {
    throw new RuntimeException("FUCK OFF :'( I DONT KNOW HOW");
  }

  public static int[] convertAToB(int[] A) {
    return A.length % 2 == 0 ? convertAToBEven(A) : convertAToBOdd(A);
  }

  public static int[] expand(int[] elements) {
    int[] y = new int[elements.length * 2];
    for (int i = 0; i < elements.length; i++) {
      int r = random.nextInt(Integer.MAX_VALUE);
      y[2 * i] = elements[i] ^ r;
      y[2 * i + 1] = r;
    }
    return y;
  }

  public static int binaryToArithmetic(int x, int r) {
    int gamma = random.nextInt(Integer.MAX_VALUE);
    int T = x ^ gamma;
    T = T - gamma;
    T = T ^ x;
    gamma = gamma ^ r;
    int A = x ^ gamma;
    A = A - gamma;
    return (A ^ T);
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

  public void sparkle256(int[][] state) {
    sparkle(state, 4, 10);
  }

  public void sparkle256Slim(int[][] state) {
    sparkle(state, 4, 7);
  }

  public void sparkle384(int[][] state) {
    sparkle(state, 6, 11);
  }

  public void sparkle384Slim(int[][] state) {
    sparkle(state, 6, 7);
  }

  public void sparkle512(int[][] state) {
    sparkle(state, 8, 12);
  }

  public void sparkle512Slim(int[][] state) {
    sparkle(state, 8, 8);
  }
}
