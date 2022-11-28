package sparkle;

import java.util.Arrays;
import java.util.Random;

/**
 * sparkle.Sparkle permutation.
 */
public final class MaskedSparkleKoggeStone implements MaskedSparkle {

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
        int toAdd0 = rot(state[0][j + 1], shiftOne);
        int toAdd1 = rot(state[1][j + 1], shiftOne);
        state[1][j] = koggeStoneMaskedAddition(toAdd1, state[1][j], state[0][j], toAdd0);
        state[0][j + 1] ^= rot(state[0][j], shiftTwo);
        state[1][j + 1] ^= rot(state[1][j], shiftTwo);
        state[1][j] ^= rc;
    }

    static int[] recoverState(int[][] state) {
        int[] result = new int[state[0].length];
        for (int i = 0; i < state[0].length; i++) {
            int resultMask = state[0][i];
            for (int j = 1; j < state.length; j++) {
                resultMask ^= state[j][i];
            }
            result[i] = resultMask;
        }
        return result;
    }


    public static int koggeStoneMaskedAddition(int x, int y, int r, int s) {
        int n = 5;
        int t = random.nextInt(Integer.MAX_VALUE);
        int u = random.nextInt(Integer.MAX_VALUE);
        int p = secXor(x, y, r);
        int g = secAnd(x, y, s, r, u);
        g = g ^ s;
        g = g ^ u;
        for (int i = 1; i < n; i++) {
            int H = secShift(g, s, t, (int) Math.pow(2, i - 1));
            int U = secAnd(p, H, s, t, u);
            g = secXor(g, U, u);
            H = secShift(p, s, t, (int) Math.pow(2, i - 1));
            p = secAnd(p, H, s, t, u);
            p = p ^ s;
            p = p ^ u;
        }
        int H = secShift(g, s, t, (int) Math.pow(2, n - 1));
        int U = secAnd(p, H, s, t, u);
        g = secXor(g, U, u);
        int z = secXor(y, x, s);
        z = z ^ (2 * g);
        z = z ^ (2 * s);
        return z;
    }

    private static int secShift(int x, int s, int t, int j) {
        int y = t ^ (x << j);
        return y ^ (s << j);
    }

    private static int secXor(int x, int y, int r) {
        int z = x ^ y;
        z = z ^ r;
        return z;
    }

    private static int secAnd(int x, int y, int s, int t, int u) {
        int z = u ^ (x & y);
        z = z ^ (x & t);
        z = z ^ (s & y);
        z = z ^ (s & t);
        return z;
    }

    @Override
    public void sparkle256(int[][] stateShares) {
        sparkle(stateShares, 4, 10);
    }

    @Override
    public void sparkle256Slim(int[][] stateShares) {
        sparkle(stateShares, 4, 7);
    }

    @Override
    public void sparkle384(int[][] stateShares) {
        sparkle(stateShares, 6, 11);
    }

    @Override
    public void sparkle384Slim(int[][] stateShares) {
        sparkle(stateShares, 6, 7);
    }

    @Override
    public void sparkle512(int[][] stateShares) {
        sparkle(stateShares, 8, 12);
    }

    @Override
    public void sparkle512Slim(int[][] stateShares) {
        sparkle(stateShares, 8, 8);
    }
}