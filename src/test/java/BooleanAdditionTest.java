import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public final class BooleanAdditionTest {

    @RepeatedTest(100)
    void booleanAndTest(){
        Random random = new Random();
        int x1 = random.nextInt(Integer.MAX_VALUE);
        int x2 = random.nextInt(Integer.MAX_VALUE);
        int y1 = random.nextInt(Integer.MAX_VALUE);
        int y2 = random.nextInt(Integer.MAX_VALUE);

        int realX = x1 ^ x2;
        int realY = y1 ^ y2;

        int realResult = realX & realY;

        int[] xShares = new int[]{x1, x2};
        int[] yShares = new int[]{y1, y2};

        int[] z = BooleanAddition.secureBooleanAnd(xShares, yShares);

        int realZ = z[0] ^ z[1];

        Assertions.assertThat(realZ).isEqualTo(realResult);
    }

    @RepeatedTest(100)
    void booleanAdditionTest(){
        Random random = new Random();
        int x1 = random.nextInt(Integer.MAX_VALUE);
        int x2 = random.nextInt(Integer.MAX_VALUE);
        int y1 = random.nextInt(Integer.MAX_VALUE);
        int y2 = random.nextInt(Integer.MAX_VALUE);

        int realX = x1 ^ x2;
        int realY = y1 ^ y2;

        int realResult = realX + realY;

        int[] xShares = new int[]{x1, x2};
        int[] yShares = new int[]{y1, y2};

        int[] z = BooleanAddition.secureBooleanAdditionGoubin(xShares, yShares);

        int realZ = z[0] ^ z[1];

        Assertions.assertThat(realZ).isEqualTo(realResult);
    }
}
