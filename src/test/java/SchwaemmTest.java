import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public final class SchwaemmTest {

  private static final String schwaemmCpath =
      SparkleTest.class.getResource("/schwaemm/schwaemmC.exe").getPath();

  private static final String schwaemmCpathLinux =
      SparkleTest.class.getResource("/schwaemm/schwaemmC").getPath();

  private static final String resourceSchwaemm =
      System.getProperty("user.dir") + "/src/test/resources/schwaemm";

  private final Random random = new Random();

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

  @AfterEach
  public void tearDown() throws IOException {
    Files.write(Path.of(resourceSchwaemm + "/key"), new byte[0]);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), new byte[0]);
    Files.write(Path.of(resourceSchwaemm + "/associate"), new byte[0]);
    Files.write(Path.of(resourceSchwaemm + "/message"), new byte[0]);
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
  void finalizeCall() throws IOException, InterruptedException {
    int randomInt = random.nextInt(32 - 1) + 1;
    int randomMsg = random.nextInt(32 - 1) + 1;
    byte[] associate = new byte[randomInt];
    byte[] message = new byte[randomMsg];
    random.nextBytes(associate);
    random.nextBytes(message);

    byte[] nonce = new byte[16];

    random.nextBytes(nonce);

    byte[] key = new byte[16];
    random.nextBytes(key);

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    int[] cState =
        callProcess(
            new String[] {
              schwaemmCpathLinux, "finalize", String.valueOf(randomInt), String.valueOf(randomMsg)
            });

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Schwaemm.encrypt(state, message);
    Schwaemm.finalize(state, key);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(50)
  void generateTag() throws IOException, InterruptedException {
    int randomInt = random.nextInt(32 - 1) + 1;
    int randomMsg = random.nextInt(32 - 1) + 1;
    byte[] associate = new byte[randomInt];
    byte[] message = new byte[randomMsg];
    random.nextBytes(associate);
    random.nextBytes(message);

    byte[] nonce = new byte[16];
    random.nextBytes(nonce);

    byte[] key = new byte[16];
    random.nextBytes(key);

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    int[] cState =
        callProcess(
            new String[] {
              schwaemmCpathLinux,
              "generateTag",
              String.valueOf(randomInt),
              String.valueOf(randomMsg)
            });
    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    byte[] javaCipher = Schwaemm.encrypt(state, message);

    Schwaemm.finalize(state, key);
    Schwaemm.generateTag(state, javaCipher, message.length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void checkSchwaemm() throws IOException, InterruptedException {
    int randomInt = random.nextInt(32 - 1) + 1;
    int randomMsg = random.nextInt(32 - 1) + 1;
    byte[] associate = new byte[randomInt];
    byte[] message = new byte[randomMsg];
    random.nextBytes(associate);
    random.nextBytes(message);

    byte[] nonce = new byte[16];

    random.nextBytes(nonce);

    byte[] key = new byte[16];
    random.nextBytes(key);

    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    callProcess(
        new String[] {
          schwaemmCpathLinux, "fullFunction", String.valueOf(randomInt), String.valueOf(randomMsg)
        });
    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));

    byte[] javaCipher = Schwaemm.encryptAndTag(message, associate, key, nonce);

    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void processPlaintext() throws IOException, InterruptedException {
    int randomInt = random.nextInt(32 - 1) + 1;
    int randomMsg = random.nextInt(32 - 1) + 1;
    byte[] associate = new byte[randomInt];
    byte[] message = new byte[randomMsg];
    random.nextBytes(associate);
    random.nextBytes(message);

    byte[] nonce = new byte[16];
    random.nextBytes(nonce);

    byte[] key = new byte[16];
    random.nextBytes(key);

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    int[] cState =
        callProcess(
            new String[] {
              schwaemmCpathLinux, "encrypt", String.valueOf(randomInt), String.valueOf(randomMsg)
            });

    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    byte[] javaCipher = Schwaemm.encrypt(state, message);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void processAssociateData() throws IOException, InterruptedException {
    int randomInt = random.nextInt(32 - 1) + 1;
    byte[] associate = new byte[randomInt];
    random.nextBytes(associate);

    byte[] nonce = new byte[16];
    random.nextBytes(nonce);

    byte[] key = new byte[16];
    random.nextBytes(key);

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
}
