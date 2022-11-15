import java.util.Arrays;
import java.util.function.Consumer;

public final class Esch {


    private final Consumer<int[]> sparkleSlim;
    private final Consumer<int[]> sparkle;

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

    public Esch(int type){
    switch (type) {
            case 256 -> {
                ESCH_DIGEST_LEN = 256;
                SPARKLE_STATE = 384;
                SPARKLE_RATE = 128;
                SPARKLE_CAPACITY = 256;
                SPARKLE_STEPS_SLIM = 7;
                SPARKLE_STEPS_BIG = 11;
                this.sparkleSlim = Sparkle::sparkle384Slim;
                this.sparkle = Sparkle::sparkle384;
            }
            case 384 -> {
                ESCH_DIGEST_LEN = 384;
                SPARKLE_STATE = 512;
                SPARKLE_RATE = 128;
                SPARKLE_CAPACITY = 384;
                SPARKLE_STEPS_SLIM = 8;
                SPARKLE_STEPS_BIG = 12;
                this.sparkleSlim = Sparkle::sparkle512Slim;
                this.sparkle = Sparkle::sparkle512;
            }
            default -> throw new RuntimeException("Unknown Esch configuration!");
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


    void add_msg_blk(int[] state, int[] in)
    {
        int tmpx = 0, tmpy = 0;

        for(int i = 0; i < RATE_WORDS; i += 2) {
            tmpx ^= in[i];
            tmpy ^= in[i+1];
        }
        tmpx = ell(tmpx);
        tmpy = ell(tmpy);
        for(int i = 0; i < RATE_WORDS; i += 2) {
            state[i] ^= (in[i] ^ tmpy);
            state[i+1] ^= (in[i+1] ^ tmpx);
        }
        for(int i = RATE_WORDS; i < (STATE_WORDS/2); i += 2) {
            state[i] ^= tmpy;
            state[i+1] ^= tmpx;
        }
    }

    void add_msg_blk_last(int[] state, int[] in, int inlen)
    {
        int tmpx = 0, tmpy = 0;
        int i;

        int[] buffer = new int[RATE_WORDS];
        System.arraycopy(in, 0, buffer, 0, in.length);

        if (inlen < RATE_BYTES) {
            buffer[inlen / 4] |= 128 << (8 * (inlen % 4));
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


    void Initialize(int[] state)
    {
        int i;

        for (i = 0; i < STATE_WORDS; i++)
            state[i] = 0;
    }

    void ProcessMessage(int[]state, byte[] in)
    {
        int length = in.length;
        int index = 0;

        int[] msgAsInt;

        //Handle empty array
        if(length > 0) {
            msgAsInt = ConversionUtil.createIntArrayFromBytes(in, (in.length - 1) / 4 + 1);
        }
        else {
            msgAsInt = new int[0];
        }


        while (length > RATE_BYTES) {
            add_msg_blk(state, Arrays.copyOfRange(msgAsInt, index, msgAsInt.length));
            sparkleSlim.accept(state);
            length -= RATE_BYTES;
            index += RATE_WORDS;
        }



        state[STATE_BRANS-1] ^= ((length < RATE_BYTES) ? CONST_M1 : CONST_M2);
        add_msg_blk_last(state, Arrays.copyOfRange(msgAsInt, index, msgAsInt.length), length);
        sparkle.accept(state);
    }

    void Finalize(int[] state, byte[] out)
    {
        int outlen;
        int outIndex = 0;



        ConversionUtil.populateByteArrayFromInts(state, out,0,  RATE_BYTES, outIndex);
        outlen = RATE_BYTES;
        outIndex += RATE_BYTES;
        while (outlen < DIGEST_BYTES) {
            sparkleSlim.accept(state);
            ConversionUtil.populateByteArrayFromInts(state, out,0, RATE_BYTES, outIndex);
            outlen += RATE_BYTES;
            outIndex += RATE_BYTES;
        }
    }

    int crypto_hash(byte[] out, byte[] in)
    {
        int[] state = new int[STATE_WORDS];

        Initialize(state);
        ProcessMessage(state, in);
        Finalize(state, out);

        return 0;
    }

}

