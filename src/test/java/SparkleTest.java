import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test java implementation with C implementation of sparkle. */
public final class SparkleTest {

  private static final String sparkleCpath = SparkleTest.class.getResource("/sparkle").getPath();

  private static final String[] arguments =
      new String[] {
        sparkleCpath,
        "-10",
        "20",
        "30",
        "1",
        "2",
        "3",
        "12",
        "2",
        "2",
        "4",
        "2",
        "1",
        "2",
        "2",
        "3",
        "1",
        "2",
        "3",
        "8",
        "6",
        "52",
        "3",
        "24",
        "3",
        "4",
        "2",
        "5",
        "7",
        "8",
        "2",
        "7",
        "4",
        "2",
        "override",
        "override"
      };

  private static final int[] javaStateFinal =
      new int[] {
        -10, 20, 30, 1, 2, 3, 12, 2, 2, 4, 2, 1, 2, 2, 3, 1, 2, 3, 8, 6, 52, 3, 24, 3, 4, 2, 5, 7,
        8, 2, 7, 4, 2
      };

  private static int[] javaState;

  @BeforeEach
  public void setup() {
    javaState = Arrays.copyOf(javaStateFinal, javaStateFinal.length);
  }

  @Test
  void sparkle256TestEmptyStartState() throws IOException, InterruptedException {
    String[] emptyState =
        new String[] {
          sparkleCpath,
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "0",
          "4",
          "10"
        };
    callProcess(Sparkle::sparkle256, new int[32], emptyState);
  }

  @Test
  void sparkle256Test() throws IOException, InterruptedException {
    callProcess(Sparkle::sparkle256);
  }

  @Test
  void sparkle256SlimTest() throws IOException, InterruptedException {
    callProcess(Sparkle::sparkle256Slim);
  }

  @Test
  void sparkle384Test() throws IOException, InterruptedException {
    callProcess(Sparkle::sparkle384);
  }

  @Test
  void sparkle384SlimTest() throws IOException, InterruptedException {
    callProcess(Sparkle::sparkle384Slim);
  }

  @Test
  void sparkle512Test() throws IOException, InterruptedException {
    callProcess(Sparkle::sparkle512);
  }

  @Test
  void sparkle512SlimTest() throws IOException, InterruptedException {
    callProcess(Sparkle::sparkle512Slim);
  }

  private static void callProcess(Consumer<int[]> function, int[] state, String[] arguments)
      throws InterruptedException, IOException {
    Process process = new ProcessBuilder(Arrays.asList(arguments)).start();
    int result = process.waitFor();
    if (result != 0) {
      printError(process);
    }
    File myObj = new File("/mnt/c/Users/tnl/sparkle2/Sparkle/test");
    byte[] bytes = Files.readAllBytes(myObj.toPath());
    int[] cState = getStateFromBytes(bytes);
    function.accept(state);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  private static void callProcess(Consumer<int[]> function)
      throws IOException, InterruptedException {
    callProcess(function, javaState, arguments);
  }

  private static int[] getStateFromBytes(byte[] bytes) {
    int intLength = bytes.length / 4;
    int[] result = new int[intLength];
    int intIndex = 0;
    for (int offset = 0; offset < intLength; offset += 4) {
      result[intIndex] = intFromBytes(bytes, offset);
      intIndex++;
    }
    return result;
  }

  private static void printError(Process process) throws IOException {
    BufferedReader errinput = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String line = errinput.readLine();
    while (line != null) {
      System.out.println(line);
      line = errinput.readLine();
    }
  }

  public static int intFromBytes(byte[] bytes, int offset) {
    return Byte.toUnsignedInt(bytes[0 + offset]) << 24
        | Byte.toUnsignedInt(bytes[1 + offset]) << 16
        | Byte.toUnsignedInt(bytes[2 + offset]) << 8
        | Byte.toUnsignedInt(bytes[3 + offset]);
  }
}
