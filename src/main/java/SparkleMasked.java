import java.util.Arrays;
import java.util.Random;

/**
 * Sparkle permutation.
 */
public final class SparkleMasked {

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

    for (int i = 0; i < steps; i++) {
      state[0][1] ^= rcon[i % maxBranches];
      state[0][3] ^= i;
      for (int k = 0; k < state.length - 1; k++) {
        for (int j = 0; j < 2 * brans; j += 2) {
          int rc = rcon[j >> 1];
          int toAdd = 0;
          for (int l = 1; l < state.length; l++) {
            toAdd += alzetteRound(state, j, 31, 24, k);
          }
          alzetteRoundLast(state, j, 31, 24, rc, toAdd);
//
//          toAdd = 0;
//          for (int l = 1; l < state.length; l++) {
//            toAdd += alzetteRound(state, j, 17, 17, k);
//          }
//          alzetteRoundLast(state, j, 17, 17, rc, toAdd);
//
//          toAdd = 0;
//          for (int l = 1; l < state.length; l++) {
//            toAdd +=  alzetteRound(state, j, 0, 31, k);
//          }
//          alzetteRoundLast(state, j, 0, 31, rc, toAdd);
//
//          toAdd = 0;
//          for (int l = 1; l < state.length; l++) {
//            toAdd +=  alzetteRound(state, j, 24, 16, k);
//          }
//          alzetteRoundLast(state, j, 24, 16, rc, toAdd);
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
    // TODO MUST BE DONE BETTER
    // First order
    if (state.length == 2) {
      int toAdd = rot(state[index][j + 1], shiftOne);
      int stateJ = binaryToArithmetic(state[index][j], state[1][j]);
      stateJ += toAdd;
      state[index][j] = arithmeticToBinary(stateJ, state[1][j]);
      state[index][j + 1] ^= rot(state[index][j], shiftTwo);
      return toAdd;
    } else {
      return 0;
    }
  }

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

  public static int[] booleanToArithmeticHigherOrder(int x[][], int index) {
    int[] maskedColumn = new int[x.length];
    for (int i = 0; i < x.length; i++) {
      maskedColumn[i] = x[i][index];
    }
    System.out.println(maskedColumn[0] ^ maskedColumn[1]);
//    System.out.println(maskedColumn[0] ^ maskedColumn[1] ^ maskedColumn[2]);
    int[] A = new int[x.length];
    int[] ADot = new int[x.length];
    for (int i = 0; i < A.length - 1; i++) {
      int rand = random.nextInt(Integer.MAX_VALUE);
      A[i] = rand;
      ADot[i] = -rand;
    }
    ADot[ADot.length - 1] = 0;
    System.out.println("ADot " + Arrays.toString(ADot));
    int[] y = convertAToB(ADot);
    System.out.println(Arrays.toString(y));
    System.out.println(Arrays.toString(maskedColumn));
    int[] z = secAdd(maskedColumn, y);
    A[A.length - 1] = fullXOR(z);
    System.out.println("A: " + Arrays.toString(A));
    return A;
  }

  // TODO MAKE SECURE
  private static int fullXOR(int[] y) {
    int res = 0;
    for (int number : y) {
      res ^= number;
    }
    return res;
  }

  public static int[] convertAToB(int[] A) {
    if (A.length == 1) {
      return new int[]{A[0]};
    }
    int[] x = convertAToB(Arrays.copyOf(A, A.length / 2));
    System.out.println("x " + Arrays.toString(x));
    int[] xDot = expand(x);
    int[] y = convertAToB(Arrays.copyOfRange(A, (A.length - 1) / 4 + 1, A.length));
    System.out.println("y " + Arrays.toString(y));
    int[] yDot = expand(y);
    System.out.println("xDot " + Arrays.toString(xDot));
    System.out.println(xDot[0] ^ xDot[1]);
    System.out.println("yDot " + Arrays.toString(yDot));
    System.out.println(yDot[0] ^ yDot[1]);
    return secAdd(xDot, yDot);
  }

  // TODO MAKE SECURE LUL AND MAKE IT WORK WAH
  private static int[] secAdd(int[] x, int[] y) {
    int length = Math.max(x.length, y.length);
    int[] result = new int[length];
    for (int i = 0; i < length; i++) {
//      if (i >= x.length) {
//        result[i] = y[i];
//
//      } else if (i >= y.length) {
//        result[i] = x[i];
//      }
//      else {
      result[i] = x[i] + y[i];
//      }
    }
    return result;
  }

  public static int[] expand(int[] x) {
    int[] y = new int[x.length * 2];
    for (int i = 0; i < x.length; i++) {
      int r = random.nextInt(Integer.MAX_VALUE);
      y[2 * i] = x[i] ^ r;
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
}
