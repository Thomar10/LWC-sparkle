package esch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import sparkle.MaskedSparkleHigherOrder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HexFormat;
import java.util.Random;

public class EschMaskedHigherOrderTest256 {

    private final int ESCH_DIGEST_LEN = 256;
    private final int DIGEST_BYTES = (ESCH_DIGEST_LEN/8);
    private final EschMasked EschMasked = new EschMasked(256, new MaskedSparkleHigherOrder());
    private final Esch Esch = new Esch(256);

    private final Random random = new Random();

    /*@Test
    void HashEmptyArray() {
        byte[] out = new byte[DIGEST_BYTES];
        byte[] in = {};

        EschJava.crypto_hash(out, in);
        assert(convertByteToHexadecimal(out).equals("C0E815D78B875DC768C6C8B3AFA51987CD69E5C087D387368628A511CFAD5730"));
    }*/

    @RepeatedTest(50)
    void finalizeCall() {
        EschHelper data = EschHelper.prepareTest(256, 5, 2048, random);
        EschHelper.MaskedData maskedData = EschHelper.convertDataToMasked(data, 3);

        Esch.finalize(data.state(), data.out());
        EschMasked.finalize(maskedData.state(), maskedData.out());

        EschHelper unmaskedData = EschHelper.recoverData(maskedData);

        Assertions.assertThat(data.out()).isEqualTo(unmaskedData.out());
    }

    @RepeatedTest(50)
    void processCall() {
        EschHelper data = EschHelper.prepareTest(256, 5, 2048, random);
        EschHelper.MaskedData maskedData = EschHelper.convertDataToMasked(data, 3);

        Esch.processMessage(data.state(), data.in());
        EschMasked.processMessage(maskedData.state(), maskedData.in());

        EschHelper unmaskedData = EschHelper.recoverData(maskedData);

        Assertions.assertThat(data.state()).isEqualTo(unmaskedData.state());
    }

    @RepeatedTest(50)
    void cryptoHashTest() {
        EschHelper data = EschHelper.prepareTest(256, 5, 2048, random);
        EschHelper.MaskedData maskedData = EschHelper.convertDataToMasked(data, 3);

        Esch.crypto_hash(data.out(), data.in());
        EschMasked.crypto_hash(maskedData.out(), maskedData.in());

        EschHelper unmaskedData = EschHelper.recoverData(maskedData);

        Assertions.assertThat(data.out()).isEqualTo(unmaskedData.out());
    }

   @Test
    void HashFromFileFirstOrder() throws FileNotFoundException {
        byte[] in = {};

        byte[][] outMasked = new byte[3][DIGEST_BYTES];
        byte[][] inMasked = {};

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("esch/LWC_HASH_KAT_256.txt").getFile());

        EschKatTestHelper testHelper = new EschKatTestHelper(file);

        String[] test;

        while(testHelper.hasNext()){
            test = testHelper.getNextTest();
            in = HexFormat.of().parseHex(test[0]);
            inMasked = EschHelper.maskByteArrays(in, 3);

            EschMasked.crypto_hash(outMasked, inMasked);

            assert(convertByteToHexadecimal(EschHelper.recoverByteArrays(outMasked)).equals(test[1]));
        }
    }

    public static String convertByteToHexadecimal(byte[] byteArray)
    {
        String hex = "";

        // Iterating through each byte in the array
        for (byte i : byteArray) {
            hex += String.format("%02X", i);
        }

        return hex;
    }
}

