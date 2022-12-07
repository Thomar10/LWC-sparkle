package sparkle;

import java.util.Random;

/**
 * sparkle.Sparkle permutation.
 */
public final class MaskedSparkleGoubin implements MaskedSparkle {

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
        int[] addRes = secAdd(toAdd1, state[1][j], toAdd0, state[0][j]);

        state[1][j] = addRes[0];
        state[0][j] = addRes[1];

        state[0][j + 1] ^= rot(state[0][j], shiftTwo);
        state[1][j + 1] ^= rot(state[1][j], shiftTwo);
        state[1][j] ^= rc;
    }

    static int[] secAdd(int x, int y, int rx, int ry){
        int C = random.nextInt(Integer.MAX_VALUE);
        int T, omega, B, A0, A1;
        T = x & y;   omega = C ^ T;
        T = x & ry;  omega = omega ^ T;
        T = y & rx;  omega = omega ^ T;
        T = rx & ry; omega = omega ^ T;

        B = omega << 1 ; C = C << 1;
        A0 = x ^ y ; A1 = rx ^ ry;
        T = C & A0; omega = omega ^ T;
        T = C & A1; omega = omega ^ T;

        for (int k = 2; k < 32; k++) {
            T = B & A0; B = B & A1;
            B = B ^ omega;
            B = B ^ T;
            B = B << 1;
        }

        A0 = A0 ^ B;
        A0 = A0 ^ C;

        int[] res = {A0, A1};
        return res;
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