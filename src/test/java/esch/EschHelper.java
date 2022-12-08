package esch;

import schwaemm.SchwaemmHelper;
import schwaemm.SchwaemmType;
import util.ConversionUtil;

import java.util.Random;

public record EschHelper(byte[] in, byte[] out, int[] state) {

    static Random random = new Random();

    public static EschHelper prepareTest(int type, int minLength, Random random) {
        int ESCH_DIGEST_LEN;
        int SPARKLE_STATE;
        switch (type) {
            case 256 -> {
                ESCH_DIGEST_LEN = 256;
                SPARKLE_STATE = 384;
            }
            case 384 -> {
                ESCH_DIGEST_LEN = 384;
                SPARKLE_STATE = 512;
            }
            default -> throw new RuntimeException("Unknown esch.Esch configuration!");
        }

        int stateLength = (SPARKLE_STATE / 32);;
        int outLength = (ESCH_DIGEST_LEN/8);

        int randomMsg = random.nextInt(32 - minLength) + minLength;
        byte[] in = new byte[randomMsg];
        random.nextBytes(in);

        byte[] out = new byte[outLength];

        int[] state = new int[stateLength];
        for (int i = 0; i < state.length; i++) {
            int randomNumber = random.nextInt(Integer.MAX_VALUE);
            state[i] = randomNumber;

        }

        return new EschHelper(in, out, state);
    }

    public static EschHelper.MaskedData convertDataToMasked(EschHelper data, int order){
        return new MaskedData(maskByteArrays(data.in, order), maskByteArrays(data.out, order), maskIntArray(data.state, order));
    }

    public static EschHelper.MaskedData convertDataToMaskedFirstOrder(EschHelper data){
        return convertDataToMasked(data, 2);
    }

    public static EschHelper recoverData(EschHelper.MaskedData data) {
        return new EschHelper(recoverByteArrays(data.in), recoverByteArrays(data.out), recoverState(data.state));
    }

    public static byte[] recoverByteArrays(byte[][] bytes) {
        // TODO FIX if length of bytes[i] == 0 better
        if (bytes[0].length == 0) {
            return new byte[0];
        }
        int[][] maskedInts = new int[bytes.length][(bytes[0].length - 1) / 4 + 1];
        for (int i = 0; i < bytes.length; i++) {
            maskedInts[i] = ConversionUtil.createIntArrayFromBytes(bytes[i], (bytes[0].length - 1) / 4 + 1);
        }
        int[] recoveredInts = recoverState(maskedInts);
        byte[] recoveredBytes = new byte[bytes[0].length];
        ConversionUtil.populateByteArrayFromInts(recoveredInts, recoveredBytes, 0,
                recoveredBytes.length, 0);
        return recoveredBytes;
    }

    public static int[][] maskIntArray(int[] ints, int order) {
        int[][] maskedState = new int[order][ints.length];
        for (int i = 1; i < order; i++) {
            for (int j = 0; j < ints.length; j++) {
                int number = random.nextInt(Integer.MAX_VALUE);
                maskedState[i][j] = number;
            }
        }
        for (int j = 0; j < ints.length; j++) {
            int resultMask = ints[j];
            for (int i = 1; i < order; i++) {
                resultMask ^= maskedState[i][j];
            }
            maskedState[0][j] = resultMask;
        }

        return maskedState;
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

    public static byte[][] maskByteArrays(byte[] bytes, int order) {
        byte[][] maskedBytes = new byte[order][];
        int[] intAsBytes = ConversionUtil.createIntArrayFromBytes(bytes, (bytes.length + 4 - 1) / 4);
        int[][] maskedInts = maskIntArray(intAsBytes, order);
        for (int i = 0; i < order; i++) {
            byte[] buffer = new byte[bytes.length];
            ConversionUtil.populateByteArrayFromInts(maskedInts[i], buffer, 0, buffer.length, 0);
            maskedBytes[i] = buffer;
        }
        return maskedBytes;
    }


    public record MaskedData(byte[][] in, byte[][] out, int[][] state) { }
}



