package esch;

import sparkle.MaskedSparkle;
import util.ConversionUtil;

import java.util.function.Consumer;

public final class EschMasked {


    private final Consumer<int[][]> sparkleSlim;
    private final Consumer<int[][]> sparkle;

    //Settings (This case is esch256)
    private final int ESCH_DIGEST_LEN;
    private final int SPARKLE_STATE;
    private final int SPARKLE_RATE;
    private final int SPARKLE_CAPACITY;
    private final int SPARKLE_STEPS_SLIM;
    private final int SPARKLE_STEPS_BIG;

    //Constants derived from settings

    private final int DIGEST_WORDS;
    private final int DIGEST_BYTES;
    private final int STATE_BRANS;
    private final int STATE_WORDS;
    private final int STATE_BYTES;
    private final int RATE_BRANS;
    private final int RATE_WORDS;
    private final int RATE_BYTES;
    private final int CAP_BRANS;
    private final int CAP_WORDS;
    private final int CAP_BYTES;

    private final int CONST_M1;
    private final int CONST_M2;

    public EschMasked(int type, MaskedSparkle maskedSparkle){
    switch (type) {
            case 256 -> {
                ESCH_DIGEST_LEN = 256;
                SPARKLE_STATE = 384;
                SPARKLE_RATE = 128;
                SPARKLE_CAPACITY = 256;
                SPARKLE_STEPS_SLIM = 7;
                SPARKLE_STEPS_BIG = 11;
                this.sparkleSlim = maskedSparkle::sparkle384Slim;
                this.sparkle = maskedSparkle::sparkle384;
            }
            case 384 -> {
                ESCH_DIGEST_LEN = 384;
                SPARKLE_STATE = 512;
                SPARKLE_RATE = 128;
                SPARKLE_CAPACITY = 384;
                SPARKLE_STEPS_SLIM = 8;
                SPARKLE_STEPS_BIG = 12;
                this.sparkleSlim = maskedSparkle::sparkle512Slim;
                this.sparkle = maskedSparkle::sparkle512;
            }
            default -> throw new RuntimeException("Unknown esch.Esch configuration!");
        }


        DIGEST_WORDS = (ESCH_DIGEST_LEN/32);
        DIGEST_BYTES = (ESCH_DIGEST_LEN/8);

        STATE_BRANS = (SPARKLE_STATE / 64);
        STATE_WORDS = (SPARKLE_STATE / 32);
        STATE_BYTES = (SPARKLE_STATE / 8);
        RATE_BRANS = (SPARKLE_RATE / 64);
        RATE_WORDS = (SPARKLE_RATE / 32);
        RATE_BYTES = (SPARKLE_RATE / 8);
        CAP_BRANS = (SPARKLE_CAPACITY / 64);
        CAP_WORDS = (SPARKLE_CAPACITY / 32);
        CAP_BYTES = (SPARKLE_CAPACITY / 8);

        CONST_M1 = (1 << 24);
        CONST_M2 = (2 << 24);

    }

    int rot(int x, int n) {
        return (x >>> n) | (x << (32 - n));
    }

    int ell(int x) {
        return rot(x ^ (x << 16), 16);
    }


    void add_msg_blk(int[] state, int[] in, int inIndex)
    {
        int tmpx = 0, tmpy = 0;

        for(int i = 0; i < RATE_WORDS; i += 2) {
            tmpx ^= in[inIndex+i];
            tmpy ^= in[inIndex+i+1];
        }
        tmpx = ell(tmpx);
        tmpy = ell(tmpy);
        for(int i = 0; i < RATE_WORDS; i += 2) {
            state[i] ^= (in[inIndex+i] ^ tmpy);
            state[i+1] ^= (in[inIndex+i+1] ^ tmpx);
        }
        for(int i = RATE_WORDS; i < (STATE_WORDS/2); i += 2) {
            state[i] ^= tmpy;
            state[i+1] ^= tmpx;
        }
    }

    void add_msg_blk_last(int[] state, int[] in, int inlen, int inIndex, boolean useConstants)
    {
        int tmpx = 0, tmpy = 0;
        int i;

        int[] buffer = new int[RATE_WORDS];
        System.arraycopy(in, inIndex, buffer, 0, in.length - inIndex);

        if(useConstants){
            if (inlen < RATE_BYTES) {
                buffer[inlen / 4] |= 128 << (8 * (inlen % 4));
            }
        }

        for(i = 0; i < RATE_WORDS; i += 2) {
            tmpx ^= buffer[i];
            tmpy ^= buffer[i+1];
        }
        tmpx = ell(tmpx);
        tmpy = ell(tmpy);
        for(i = 0; i < RATE_WORDS; i += 2) {
            state[i] ^= (buffer[i] ^ tmpy);
            state[i+1] ^= (buffer[i+1] ^ tmpx);
        }
        for(i = RATE_WORDS; i < (STATE_WORDS/2); i += 2) {
            state[i] ^= tmpy;
            state[i+1] ^= tmpx;
        }
    }

    void processMessage(int[][] state, byte[][] in)
    {
        int length = in[0].length;
        int index = 0;

        int[][] msgAsInt = new int[state.length][];

        //Handle empty array
        if(length > 0) {
            for (int i = 0; i < in.length; i++) {
                msgAsInt[i] = ConversionUtil.createIntArrayFromBytes(in[i], (in[i].length - 1) / 4 + 1);
            }
        }
        else{
            for (int i = 0; i < in.length; i++) {
                msgAsInt[i] = new int[0];
            }
        }

        while (length > RATE_BYTES) {
            for (int i = 0; i < state.length; i++) {
                add_msg_blk(state[i], msgAsInt[i], index);
            }
            sparkleSlim.accept(state);
            length -= RATE_BYTES;
            index += RATE_WORDS;
        }

        state[0][STATE_BRANS-1] ^= ((length < RATE_BYTES) ? CONST_M1 : CONST_M2);
        add_msg_blk_last(state[0], msgAsInt[0], length, index, true);
        for (int i = 1; i < state.length; i++) {
            add_msg_blk_last(state[i], msgAsInt[i], length, index, false);
        }
        sparkle.accept(state);
    }

    void finalize(int[][] state, byte[][] out)
    {
        int outlen;
        int outIndex = 0;


        for (int i = 0; i < state.length; i++) {
            ConversionUtil.populateByteArrayFromInts(state[i], out[i],0,  RATE_BYTES, outIndex);
        }
        outlen = RATE_BYTES;
        outIndex += RATE_BYTES;
        while (outlen < DIGEST_BYTES) {
            sparkleSlim.accept(state);
            for (int i = 0; i < state.length; i++) {
                ConversionUtil.populateByteArrayFromInts(state[i], out[i], 0, RATE_BYTES, outIndex);
            }
            outlen += RATE_BYTES;
            outIndex += RATE_BYTES;
        }
    }

    int crypto_hash(byte[][] out, byte[][] in)
    {
        int[][] state = new int[out.length][STATE_WORDS];



        processMessage(state, in);
        finalize(state, out);

        return 0;
    }

}

