import java.util.Arrays;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public final class EschTest256 {

    private final int ESCH_DIGEST_LEN = 256;
    private final int DIGEST_BYTES = (ESCH_DIGEST_LEN/8);
    private final Esch EschJava = new Esch(256);

    @Test
    void HashEmptyArray() {
        byte[] out = new byte[DIGEST_BYTES];
        byte[] in = {};


        System.out.println(Arrays.toString(out));
        EschJava.crypto_hash(out, in);
        System.out.println(Arrays.toString(out));
    }
}
