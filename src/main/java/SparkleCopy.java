import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sparkle permutation.
 */
public final class SparkleCopy {

  Random random = new Random();
  public static final int maxBranches = 8;
  private final int[] state;
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
  private final ConcurrentHashMap<String, Integer> map;

  public SparkleCopy(int[] state, ConcurrentHashMap<String, Integer> map) {
    this.state = state;
    this.map = map;
  }

  public void sparkle256() {
    sparkle(this.state, 4, 10);
  }

  static int rot(int x, int n) {
    return (x >>> n) | (x << (32 - n));
  }

  static int ell(int x) {
    return rot(x ^ (x << 16), 16);
  }

  private void sparkle(int[] state, int brans, int steps) {
    int rc, tmpx, tmpy, x0, y0;
    for (int i = 0; i < steps; i++) {
      state[1] ^= rcon[i % maxBranches];
      state[3] ^= i;
      for (int j = 0; j < 2 * brans; j += 2) {
        rc = rcon[j >> 1];
        alzetteRound2(state, j, 31, 24, rc, i);
        alzetteRound2(state, j, 17, 17, rc, i);
        alzetteRound2(state, j, 0, 31, rc, i);
        alzetteRound2(state, j, 24, 16, rc, i);
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

  void alzetteRound(int[] state, int j, int shiftOne, int shiftTwo, int rc, int i) {
    // Let state[j] be x and state[j+1] be y
    int toAdd = rot(state[j + 1], shiftOne);

    String toAddId = String.valueOf(j) + i + shiftOne + shiftTwo + "toAdd";
    String shareId = String.valueOf(j) + i + shiftOne + shiftTwo + "share";
    while (!map.containsKey(toAddId)) {
    }
    while (!map.containsKey(shareId)) {
    }
    int otherToAdd = map.get(toAddId);
    int share = map.get(shareId);

    addArithmeticToBinary(toAdd, otherToAdd, share, j);
    state[j + 1] ^= rot(state[j], shiftTwo);
    state[j] ^= rc;
  }

  void alzetteRound2(int[] state, int j, int shiftOne, int shiftTwo, int rc, int i) {
    // Let state[j] be x and state[j+1] be y
    int toAdd = rot(state[j + 1], shiftOne);

    String toAddId = String.valueOf(j) + i + shiftOne + shiftTwo + "toAdd";
    String shareId = String.valueOf(j) + i + shiftOne + shiftTwo + "share";
    map.put(String.valueOf(j) + i + shiftOne + shiftTwo + "shareS", state[j]);
    while (!map.containsKey(toAddId)) {
    }
    while (!map.containsKey(shareId)) {
    }
    int otherToAdd = map.get(toAddId);
    int share = map.get(shareId);

    addArithmeticToBinary2(toAdd, otherToAdd, share, j);
    state[j + 1] ^= rot(state[j], shiftTwo);
    state[j] ^= rc;
  }

  public int binaryToArithmetic(int x, int r) {
    int gamma = random.nextInt(Integer.MAX_VALUE);
    int T = x ^ gamma;
    T = T - gamma;
    T = T ^ x;
    gamma = gamma ^ r;
    int A = x ^ gamma;
    A = A - gamma;
    return A ^ T;
  }

  void addArithmeticToBinary(int toAdd, int otherToAdd, int otherShares, int j) {
    //Convert to arithmetic
    state[j] = binaryToArithmetic(state[j], otherShares);
    int toAddArith = binaryToArithmetic(toAdd, otherToAdd);

    state[j] += toAddArith + otherToAdd;

    //Convert to binary
    state[j] = arithmeticToBinary(state[j], otherShares);
  }

  void addArithmeticToBinary2(int toAdd, int otherToAdd, int otherShares, int j) {
    //Convert to arithmetic
    int stateJ = binaryToArithmetic(state[j], otherShares);
    int toAddArith = binaryToArithmetic(toAdd, otherToAdd);

    state[j] = stateJ + toAddArith;

    //Convert to binary
    state[j] = arithmeticToBinary(state[j], otherShares);
  }

  public int arithmeticToBinary(int A, int r) {
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
