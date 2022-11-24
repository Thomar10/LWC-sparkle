package esch;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

public final class EschTest256 {

    private final int ESCH_DIGEST_LEN = 256;
    private final int DIGEST_BYTES = (ESCH_DIGEST_LEN/8);
    private final Esch EschJava = new Esch(256);

    @Test
    void HashEmptyArray() {
        byte[] out = new byte[DIGEST_BYTES];
        byte[] in = {};

        EschJava.crypto_hash(out, in);
        assert(convertByteToHexadecimal(out).equals("C0E815D78B875DC768C6C8B3AFA51987CD69E5C087D387368628A511CFAD5730"));
    }

    @Test
    void HashKnownArrays() {
        byte[] out = new byte[DIGEST_BYTES];
        byte[] in = {0};

        EschJava.crypto_hash(out, in);
        assert(convertByteToHexadecimal(out).equals("D515FD9C2852D9D6F00C9CF01D858AF467EEDF21FF68CC14C005B3EFF7A6ECD3"));

        in = HexFormat.of().parseHex("0001");
        EschJava.crypto_hash(out, in);
        assert(convertByteToHexadecimal(out).equals("FBCAD7AB77FD4CC844534D2716D08C092B40B86E00647ECAA429AFDFE3B3FC43"));
    }

    @Test
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
