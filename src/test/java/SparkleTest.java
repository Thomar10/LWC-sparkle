import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test java implementation with C implementation of sparkle. */
public final class SparkleTest {

  private static final String sparkleCpath =
      SparkleTest.class.getResource("/sparkle/sparkleC").getPath();

  private static final String resourceSparkle =
      System.getProperty("user.dir") + "/src/test/resources/sparkle";


  @Test
  void sparkle256Test() throws IOException, InterruptedException {
    StateAndArgument saa = StateAndArgument.generateRandomStateAndArguments(4, 10);
    int[] cState = callProcess(saa.arguments);
    Sparkle.sparkle256(saa.state);
    Assertions.assertThat(cState).isEqualTo(saa.state);
  }

  @Test
  void sparkle256SlimTest() throws IOException, InterruptedException {
    StateAndArgument saa = StateAndArgument.generateRandomStateAndArguments(4, 7);
    int[] cState = callProcess(saa.arguments);
    Sparkle.sparkle256Slim(saa.state);
    Assertions.assertThat(cState).isEqualTo(saa.state);
  }

  @Test
  void sparkle384Test() throws IOException, InterruptedException {
    StateAndArgument saa = StateAndArgument.generateRandomStateAndArguments(6, 11);
    int[] cState = callProcess(saa.arguments);
    Sparkle.sparkle384(saa.state);
    Assertions.assertThat(cState).isEqualTo(saa.state);
  }

  @Test
  void sparkle384SlimTest() throws IOException, InterruptedException {
    StateAndArgument saa = StateAndArgument.generateRandomStateAndArguments(6, 7);
    int[] cState = callProcess(saa.arguments);
    Sparkle.sparkle384Slim(saa.state);
    Assertions.assertThat(cState).isEqualTo(saa.state);
  }

  @Test
  void sparkle512Test() throws IOException, InterruptedException {
    StateAndArgument saa = StateAndArgument.generateRandomStateAndArguments(8, 12);
    int[] cState = callProcess(saa.arguments);
    Sparkle.sparkle512(saa.state);
    Assertions.assertThat(cState).isEqualTo(saa.state);
  }

  @Test
  void sparkle512SlimTest() throws IOException, InterruptedException {
    StateAndArgument saa = StateAndArgument.generateRandomStateAndArguments(8, 8);
    int[] cState = callProcess(saa.arguments);
    Sparkle.sparkle512Slim(saa.state);
    Assertions.assertThat(cState).isEqualTo(saa.state);
  }

  private static int[] callProcess(String[] arguments)
      throws InterruptedException, IOException {
    Process process =
        new ProcessBuilder(Arrays.asList(arguments)).directory(new File(resourceSparkle)).start();
    int result = process.waitFor();
    if (result != 0) {
      printError(process);
    }
    File myObj = new File(resourceSparkle + "/test");
    Scanner scanner = new Scanner(myObj);
    int[] cState = new int[Sparkle.maxBranches * 2];
    int i = 0;
    while (scanner.hasNextInt()) {
      cState[i] = scanner.nextInt();
      i++;
    }
    return cState;
  }

  private static void printError(Process process) throws IOException {
    BufferedReader errinput = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String line = errinput.readLine();
    while (line != null) {
      System.out.println(line);
      line = errinput.readLine();
    }
  }

  /**
   * The arguments for C sparkle is as follows, on index 0 the absolute path to the C program.
   * The second last is branches, and last is steps. In between the state should be the same as java.
   *
   * @param state java state for sparkle
   * @param arguments arguments for C sparkle
   */
  record StateAndArgument(int[] state, String[] arguments) {
    static StateAndArgument generateRandomStateAndArguments(int branches, int steps) {
      Random random = new Random();
      int[] state = new int[Sparkle.maxBranches * 2];
      String[] arguments = new String[Sparkle.maxBranches * 2 + 3];
      arguments[0] = sparkleCpath;
      for (int i = 0; i < state.length; i++) {
        int randomNumber = random.nextInt(Integer.MAX_VALUE);
        state[i] = randomNumber;
        arguments[i + 1] = String.valueOf(randomNumber);
      }
      arguments[arguments.length - 2] = String.valueOf(branches);
      arguments[arguments.length - 1] = String.valueOf(steps);
      return new StateAndArgument(state, arguments);
    }
  }
}
