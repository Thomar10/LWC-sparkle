import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test java implementation with C implementation of sparkle. */
public final class SparkleTest {

  private static final String sparkleCpath =
      SparkleTest.class.getResource("/sparkleDir/sparkle").getPath();

  private static final String resourceSparkle =
      System.getProperty("user.dir") + "/src/test/resources/sparkleDir";

  private static final String[] argumentsToOverride =
      new String[] {
        sparkleCpath,
        "10",
        "21238",
        "57",
        "38",
        "75",
        "92",
        "71",
        "234",
        "96",
        "72",
        "94",
        "72",
        "9739",
        "59397",
        "234",
        "1231",
        "override",
        "override"
      };

  private static final int[] javaStateFinal =
      new int[] {10, 21238, 57, 38, 75, 92, 71, 234, 96, 72, 94, 72, 9739, 59397, 234, 1231};

  private static int[] javaState;
  private static String[] arguments;

  @BeforeEach
  public void setup() {
    javaState = Arrays.copyOf(javaStateFinal, javaStateFinal.length);
    arguments = Arrays.copyOf(argumentsToOverride, argumentsToOverride.length);
  }

  @Test
  void sparkle256Test() throws IOException, InterruptedException {
    arguments[arguments.length - 2] = "4";
    arguments[arguments.length - 1] = "10";
    callProcess(Sparkle::sparkle256);
  }

  @Test
  void sparkle256SlimTest() throws IOException, InterruptedException {
    arguments[arguments.length - 2] = "4";
    arguments[arguments.length - 1] = "7";
    callProcess(Sparkle::sparkle256Slim);
  }

  @Test
  void sparkle384Test() throws IOException, InterruptedException {
    arguments[arguments.length - 2] = "6";
    arguments[arguments.length - 1] = "11";
    callProcess(Sparkle::sparkle384);
  }

  @Test
  void sparkle384SlimTest() throws IOException, InterruptedException {
    arguments[arguments.length - 2] = "6";
    arguments[arguments.length - 1] = "7";
    callProcess(Sparkle::sparkle384Slim);
  }

  @Test
  void sparkle512Test() throws IOException, InterruptedException {
    arguments[arguments.length - 2] = "8";
    arguments[arguments.length - 1] = "12";
    callProcess(Sparkle::sparkle512);
  }

  @Test
  void sparkle512SlimTest() throws IOException, InterruptedException {
    arguments[arguments.length - 2] = "8";
    arguments[arguments.length - 1] = "8";
    callProcess(Sparkle::sparkle512Slim);
  }

  private static void callProcess(Consumer<int[]> function, int[] state, String[] arguments)
      throws InterruptedException, IOException {
    Process process =
        new ProcessBuilder(Arrays.asList(arguments)).directory(new File(resourceSparkle)).start();
    int result = process.waitFor();
    if (result != 0) {
      printError(process);
    }
    File myObj = new File(resourceSparkle + "/test");
    Scanner scanner = new Scanner(myObj);
    int[] cState = new int[16];
    int i = 0;
    while (scanner.hasNextInt()) {
      cState[i] = scanner.nextInt();
      i++;
    }
    function.accept(state);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  private static void callProcess(Consumer<int[]> function)
      throws IOException, InterruptedException {
    callProcess(function, javaState, arguments);
  }

  private static void printError(Process process) throws IOException {
    BufferedReader errinput = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String line = errinput.readLine();
    while (line != null) {
      System.out.println(line);
      line = errinput.readLine();
    }
  }
}
