public class MaskedSparkleBoolean {
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

    public static void sparkle256(int[][] stateShares) {
        sparkle(stateShares, 4, 10);
    }

    public static void sparkle256Slim(int[][] stateShares) {
        sparkle(stateShares, 4, 7);
    }

    public static void sparkle384(int[][] stateShares) {
        sparkle(stateShares, 6, 11);
    }

    public static void sparkle384Slim(int[][] stateShares) {
        sparkle(stateShares, 6, 7);
    }

    public static void sparkle512(int[][] stateShares) {
        sparkle(stateShares, 8, 12);
    }

    public static void sparkle512Slim(int[][] stateShares) {
        sparkle(stateShares, 8, 8);
    }

    static int rot(int x, int n) {
        return (x >>> n) | (x << (32 - n));
    }

    static int ell(int x) {
        return rot(x ^ (x << 16), 16);
    }

    private static void sparkle(int[][] stateShares, int brans, int steps) {
        int rc, tmpx, tmpy, x0, y0;
        for (int i = 0; i < steps; i++) {
            stateShares[0][1] ^= rcon[i % maxBranches]; //Only XOR by constant to first share or every odd share
            stateShares[0][3] ^= i;
            for (int j = 0; j < 2 * brans; j += 2) {
                rc = rcon[j >> 1];
                alzetteRound(stateShares, j, 31, 24, rc);
                alzetteRound(stateShares, j, 17, 17, rc);
                alzetteRound(stateShares, j, 0, 31, rc);
                alzetteRound(stateShares, j, 24, 16, rc);
            }

            for(int[] state : stateShares){
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
    }

    private static void sparkleInverse(int[] state, int brans, int steps) {
        int rc, tmpx, tmpy, xb1, yb1;

        for (int i = steps - 1; i >= 0; i--) {
            tmpx = tmpy = 0;
            xb1 = state[brans - 2];
            yb1 = state[brans - 1];
            for (int j = brans - 2; j > 0; j -= 2) {
                tmpx ^= (state[j] = state[j + brans]);
                state[j + brans] = state[j - 2];
                state[j + 1] = state[j + brans + 1];
                tmpy ^= state[j + 1];
                state[j + brans + 1] = state[j - 1];
            }
            tmpx ^= (state[0] = state[brans]);
            state[brans] = xb1;
            tmpy ^= (state[1] = state[brans + 1]);
            state[brans + 1] = yb1;
            tmpx = ell(tmpx);
            tmpy = ell(tmpy);
            for (int j = brans - 2; j >= 0; j -= 2) {
                state[j + brans] ^= (tmpy ^ state[j]);
                state[j + brans + 1] ^= (tmpx ^ state[j + 1]);
            }

            for (int j = 0; j < 2 * brans; j += 2) {
                rc = rcon[j >> 1];
                alzetteRoundInverse(state, j, 16, 24, rc);
                alzetteRoundInverse(state, j, 31, 0, rc);
                alzetteRoundInverse(state, j, 17, 17, rc);
                alzetteRoundInverse(state, j, 24, 31, rc);
            }
            // Add round constant
            state[1] ^= rcon[i % maxBranches];
            state[3] ^= i;
        }
    }

    static void alzetteRoundInverse(int[] state, int j, int shiftOne, int shiftTwo, int rc) {
        state[j] ^= rc;
        state[j + 1] ^= rot(state[j], shiftOne);
        state[j] -= rot(state[j + 1], shiftTwo);
    }

    static void alzetteRound(int[][] stateShares, int j, int shiftOne, int shiftTwo, int rc) {
        // Let state[j] be x and state[j+1] be y
        int[] shares = new int[stateShares.length];
        int[] toAdd = new int[stateShares.length];
        for(int i = 0; i < stateShares.length; i++){
            shares[i] = stateShares[i][j];
            toAdd[i] = rot(stateShares[i][j + 1], shiftOne);
        }

        int[] newShares = BooleanAddition.secureBooleanAdditionGoubin(shares, toAdd);

        for(int i = 0; i < stateShares.length; i++){
            stateShares[i][j] = newShares[i];
            stateShares[i][j + 1] ^= rot(stateShares[i][j], shiftTwo);
        }

        stateShares[0][j] ^= rc; //Only XOR by constant to first share or every odd share
    }
}
