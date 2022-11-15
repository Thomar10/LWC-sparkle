import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HexFormat;

public final class EschTest384 {

    private final int ESCH_DIGEST_LEN = 384;
    private final int DIGEST_BYTES = (ESCH_DIGEST_LEN/8);
    private final Esch EschJava = new Esch(384);

    @Test
    void HashEmptyArray() {
        byte[] out = new byte[DIGEST_BYTES];
        byte[] in = {};

        EschJava.crypto_hash(out, in);
        assert(convertByteToHexadecimal(out).equals("2981715E2263EBD0CB6E5C2C99D0776D5E691EE737FDE05247895E75D02E7447FD6AB707E2EC8385A539777965E472EE"));
    }

    @Test
    void HashKnownArrays() {
        byte[] out = new byte[DIGEST_BYTES];
        byte[] in = {0};

        EschJava.crypto_hash(out, in);
        assert(convertByteToHexadecimal(out).equals("CA78366C86E82726C19EBD1DBBB1375CEF93C570F856CE2FF5DA0CA87140DACD65F3E1C5AF5F84B3F6390B9AC1A2FA4D"));

        in = HexFormat.of().parseHex("0001");
        EschJava.crypto_hash(out, in);
        assert(convertByteToHexadecimal(out).equals("76A4F5B45A6062DE68F974824FCC7DE8CE4BD9CE64CE9A8958A3409151B2481D13B5D9C1BDCA1A658D31110088C54922"));
    }

    @Test
    void HashFromFile() throws FileNotFoundException {
        byte[] out = new byte[DIGEST_BYTES];
        byte[] in = {};

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("esch/LWC_HASH_KAT_384.txt").getFile());

        EschKatTestHelper testHelper = new EschKatTestHelper(file);

        String[] test;

        while(testHelper.hasNext()){
            test = testHelper.getNextTest();
            in = HexFormat.of().parseHex(test[0]);
            EschJava.crypto_hash(out, in);
            assert(convertByteToHexadecimal(out).equals(test[1]));
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
