/**
 * Sparkle permutation.
 */
public final class Sparkle {

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
    sparkle(state, 4, 10);
  }

  public static void sparkle256Slim(int[] state) {
    sparkle(state, 4, 7);
  }

  public static void sparkle384(int[] state) {
    sparkle(state, 6, 11);
  }

  public static void sparkle384Slim(int[] state) {
    sparkle(state, 6, 7);
  }

  public static void sparkle512(int[] state) {
    sparkle(state, 8, 12);
  }

  public static void sparkle512Slim(int[] state) {
    sparkle(state, 8, 8);
  }

  static int rot(int x, int n) {
    return (x >>> n) | (x << (32 - n));
  }

  static int ell(int x) {
    return rot(x ^ (x << 16), 16);
  }

  private static void sparkle(int[] state, int brans, int steps) {
    int rc, tmpx, tmpy, x0, y0;
    for (int i = 0; i < steps; i++) {
      state[1] ^= rcon[i % maxBranches];
      state[3] ^= i;
      for (int j = 0; j < 2 * brans; j += 2) {
        rc = rcon[j >> 1];
        alzetteRound(state, j, 31, 24, rc);
        alzetteRound(state, j, 17, 17, rc);
        alzetteRound(state, j, 0, 31, rc);
        alzetteRound(state, j, 24, 16, rc);
      }
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
  }

  static void alzetteRound(int[] state, int j, int shiftOne, int shiftTwo, int rc) {
    // Let state[j] be x and state[j+1] be y
    int toAdd = rot(state[j + 1], shiftOne);
    state[j] = state[j] + toAdd;
    state[j + 1] ^= rot(state[j], shiftTwo);
    state[j] ^= rc;
  }
}
