package esch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import schwaemm.SchwaemmHelper;
import schwaemm.SchwaemmType;
import sparkle.MaskedSparkleFirstOrder;

import java.util.HexFormat;
import java.util.Random;

public class EschMaskedTest256 {

    private final int ESCH_DIGEST_LEN = 256;
    private final int DIGEST_BYTES = (ESCH_DIGEST_LEN/8);
    private final EschMasked EschMasked = new EschMasked(256, new MaskedSparkleFirstOrder());
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
        EschHelper data = EschHelper.prepareTest(256, 5, random);
        EschHelper.MaskedData maskedData = EschHelper.convertDataToMaskedFirstOrder(data);

        Esch.finalize(data.state(), data.out());
        EschMasked.finalize(maskedData.state(), maskedData.out());

        EschHelper unmaskedData = EschHelper.recoverData(maskedData);

        Assertions.assertThat(data.out()).isEqualTo(unmaskedData.out());
    }

    @RepeatedTest(50)
    void processCall() {
        EschHelper data = EschHelper.prepareTest(256, 5, random);
        EschHelper.MaskedData maskedData = EschHelper.convertDataToMaskedFirstOrder(data);

        Esch.processMessage(data.state(), data.in());
        EschMasked.processMessage(maskedData.state(), maskedData.in());

        EschHelper unmaskedData = EschHelper.recoverData(maskedData);

        Assertions.assertThat(data.state()).isEqualTo(unmaskedData.state());
    }

    @RepeatedTest(50)
    void cryptoHashTest() {
        EschHelper data = EschHelper.prepareTest(256, 5, random);
        EschHelper.MaskedData maskedData = EschHelper.convertDataToMaskedFirstOrder(data);

        Esch.crypto_hash(data.out(), data.in());
        EschMasked.crypto_hash(maskedData.out(), maskedData.in());

        EschHelper unmaskedData = EschHelper.recoverData(maskedData);

        Assertions.assertThat(data.out()).isEqualTo(unmaskedData.out());
    }

    @Test
    void HashKnownArrays() {
        byte[][] outMasked = new byte[2][DIGEST_BYTES];
        byte[] outUnmasked;
        byte[] in = {0};
        byte[][] inMasked;


        inMasked = SchwaemmHelper.maskByteFirstOrder(in);
        EschMasked.crypto_hash(outMasked, inMasked);
        outUnmasked = SchwaemmHelper.recoverByteArrays(outMasked);
        System.out.println(convertByteToHexadecimal(outUnmasked));
        assert(convertByteToHexadecimal(outUnmasked).equals("D515FD9C2852D9D6F00C9CF01D858AF467EEDF21FF68CC14C005B3EFF7A6ECD3"));

        in = HexFormat.of().parseHex("0001");
        inMasked = SchwaemmHelper.maskByteFirstOrder(in);
        EschMasked.crypto_hash(outMasked, inMasked);
        outUnmasked = SchwaemmHelper.recoverByteArrays(outMasked);
        assert(convertByteToHexadecimal(outUnmasked).equals("FBCAD7AB77FD4CC844534D2716D08C092B40B86E00647ECAA429AFDFE3B3FC43"));
    }

   /* @Test
    void HashFromFile() throws FileNotFoundException {
        byte[] out = new byte[DIGEST_BYTES];
        byte[] in = {};

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("esch/LWC_HASH_KAT_256.txt").getFile());

        EschKatTestHelper testHelper = new EschKatTestHelper(file);

        String[] test;

        while(testHelper.hasNext()){
            test = testHelper.getNextTest();
            in = HexFormat.of().parseHex(test[0]);
            EschJava.crypto_hash(out, in);
            assert(convertByteToHexadecimal(out).equals(test[1]));
        }
    }*/

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

