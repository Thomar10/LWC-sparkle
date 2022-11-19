import java.util.Random;

public class MaskedSparkle {
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

    static void alzetteRound(int[] state, int j, int shiftOne, int shiftTwo, int rc) {
        // Let state[j] be x and state[j+1] be y
        state[j] = secureAdditionGoubin(state[j], rot(state[j + 1], shiftOne));
        state[j + 1] ^= rot(state[j], shiftTwo);
        state[j] ^= rc;
    }

    static int secureAnd(int x, int y){
        Random random = new Random();
        int n = 32;
        int[][] r = new int[n][n];

        for(int i = 0; i < n; i++){
            for(int j = i + 1; j < n; j++){
                r[i][j] = random.nextInt(2);
                r[j][i] = (r[i][j] ^ (getBit(x, i) & getBit(y, j))) ^ (getBit(x, j) & getBit(y, i));
            }
        }

        int z = 0;

        for(int i = 0; i < n; i++){
            int bit = getBit(x, i) & getBit(y, i);
            z = setBit(z, bit, i);

            for(int j = 0; j < n; j++){
                if(i != j){
                    bit = getBit(z, i) ^ r[i][j];
                    z = setBit(z, bit, i);
                }
            }
        }

        return z;
    }

    static int getBit(int val, int pos){
        int mask = 1 << pos;
        return ((val & mask) > 0) ? 1 : 0;
    }

    //Yes
    static int setBit(int target, int bit, int pos){
        return (target & ~(1 << pos)) | (bit << pos);
    }
    static void secureAddition(int x, int y){

    }

    static int secureAdditionGoubin(int x, int y){
        int w = secureAnd(x, y);
        int u = 0;
        int a = x ^ y;

        int k = 2; //What is k? Maybe shares
        for(int j = 0; j < k - 1; j++){
            int ua = secureAnd(u, a);
            u = ua ^ w;
            u = 2*u;
        }

        return x ^ y ^ u;
    }
}
