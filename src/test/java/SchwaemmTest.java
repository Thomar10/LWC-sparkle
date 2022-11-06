import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public final class SchwaemmTest {

  private static final String schwaemmCpath =
      SparkleTest.class.getResource("/schwaemm/schwaemmC").getPath();

  private static final String resourceSchwaemm =
      System.getProperty("user.dir") + "/src/test/resources/schwaemm";

  private final Random random = new Random();

  @AfterEach
  public void tearDown() throws IOException {
    // Files.write(Path.of(resourceSchwaemm + "/key"), new byte[0]);
    // Files.write(Path.of(resourceSchwaemm + "/nonce"), new byte[0]);
    // Files.write(Path.of(resourceSchwaemm + "/associate"), new byte[0]);
    // Files.write(Path.of(resourceSchwaemm + "/message"), new byte[0]);
  }

  @RepeatedTest(50)
  void initializeTest() throws IOException, InterruptedException {
    byte[] nonce = new byte[16];
    random.nextBytes(nonce);
    byte[] key = new byte[16];
    random.nextBytes(key);
    int[] state = new int[8];

    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    int[] cState = callProcess(new String[] {schwaemmCpath, "initialize"});

    Schwaemm.initialize(state, key, nonce);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @Test
  void initializeFails() throws IOException, InterruptedException {
    byte[] nonce = new byte[] {-16, 63, 29, 26, 85, -50, 1, 30, 69, -57, 69, 26, 13, 55, -57, -6};

    byte[] key =
        new byte[] {-46, -29, -126, -115, 64, -39, -56, 122, 74, -109, -47, 94, 103, -32, 51, 12};
    int[] state = new int[8];

    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    int[] cState = callProcess(new String[] {schwaemmCpath, "initialize"});

    Schwaemm.initialize(state, key, nonce);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(50)
  void processAssociateData() throws IOException, InterruptedException {
    int randomInt = random.nextInt(32 - 1) + 1;
    byte[] associate = new byte[randomInt];
    random.nextBytes(associate);
    System.out.println(Arrays.toString(associate));

    byte[] nonce = new byte[16];
    random.nextBytes(nonce);
    System.out.println(Arrays.toString(nonce));

    byte[] key = new byte[16];
    random.nextBytes(key);
    System.out.println(Arrays.toString(key));

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    int[] cState =
        callProcess(new String[] {schwaemmCpath, "associate", String.valueOf(randomInt)});

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @Test
  void processAssociateDataHardcoded() throws IOException, InterruptedException {
    byte[] associate =
        new byte[] {
          75, 9, 44, 96, 54, -41, -82, -22, 45, -58, 2, 88, 4, 94, -121, 32, 114, -40, 35, -64, 114,
          18, 111, 72, -50, 76, 43, -108, -63, 7, -90, -38
        };
    // random.nextBytes(associate);

    byte[] nonce = new byte[] {-16, 63, 29, 26, 85, -50, 1, 30, 69, -57, 69, 26, 13, 55, -57, -6};
    // random.nextBytes(nonce);

    byte[] key =
        new byte[] {-46, -29, -126, -115, 64, -39, -56, 122, 74, -109, -47, 94, 103, -32, 51, 12};
    // random.nextBytes(key);

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    int[] cState = callProcess(new String[] {schwaemmCpath, "associate"});

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  private static int[] callProcess(String[] arguments) throws InterruptedException, IOException {
    Process process =
        new ProcessBuilder(Arrays.asList(arguments)).directory(new File(resourceSchwaemm)).start();
    int result = process.waitFor();
    if (result != 0) {
      printError(process);
    }
    File myObj = new File(resourceSchwaemm + "/schwaemm");
    Scanner scanner = new Scanner(myObj);
    int[] cState = new int[Sparkle.maxBranches];
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
}
