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
    int[] cState = callProcess(new String[]{schwaemmCpath, "initialize"});

    Schwaemm.initialize(state, key, nonce);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @Test
  void initializeFails() throws IOException, InterruptedException {
    byte[] nonce = new byte[]{-16, 63, 29, 26, 85, -50, 1, 30, 69, -57, 69, 26, 13, 55, -57, -6};

    byte[] key =
        new byte[]{-46, -29, -126, -115, 64, -39, -56, 122, 74, -109, -47, 94, 103, -32, 51, 12};
    int[] state = new int[8];

    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    int[] cState = callProcess(new String[]{schwaemmCpath, "initialize"});

    Schwaemm.initialize(state, key, nonce);
    Assertions.assertThat(cState).isEqualTo(state);
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
            new String[]{
                schwaemmCpath, "encrypt", String.valueOf(randomInt), String.valueOf(randomMsg)
            });

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Schwaemm.encrypt(state, message);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @Test
  void processPlaintextHardcodedWithCipher() throws IOException, InterruptedException {
    byte[] associate = new byte[]{86, -6, 98, 34, 123, -126, -101, 61, -49, 94, -44, 115, 78, -63,
        -104, 74, 105, 3, 94};
    byte[] message = new byte[]{-85, 23, 47, 88, 90, 30, 16, -78};

    byte[] nonce = new byte[]{8, 104, 37, -84, -121, 127, 40, 14, -12, -31, 59, -78, 69, 79, 79,
        -121};

    byte[] key = new byte[]{-38, -55, -67, -41, -105, -47, -80, 112, -76, 17, 38, 28, 95, 12, 75,
        82};

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    int[] cState =
        callProcess(
            new String[]{
                schwaemmCpath, "encrypt", String.valueOf(19), String.valueOf(8)
            });
    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));
    System.out.println(Arrays.toString(cipher));

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Schwaemm.encrypt(state, message);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @Test
  void processPlaintextHardcoded() throws IOException, InterruptedException {
    byte[] associate = new byte[]{86, -6, 98, 34, 123, -126, -101, 61, -49, 94, -44, 115, 78, -63,
        -104, 74, 105, 3, 94};
    byte[] message = new byte[]{-85, 23, 47, 88, 90, 30, 16, -78};

    byte[] nonce = new byte[]{8, 104, 37, -84, -121, 127, 40, 14, -12, -31, 59, -78, 69, 79, 79,
        -121};

    byte[] key = new byte[]{-38, -55, -67, -41, -105, -47, -80, 112, -76, 17, 38, 28, 95, 12, 75,
        82};

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    int[] cState =
        callProcess(
            new String[]{
                schwaemmCpath, "encrypt", String.valueOf(19), String.valueOf(8)
            });

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Schwaemm.encrypt(state, message);
    Assertions.assertThat(cState).isEqualTo(state);

    associate = new byte[]{65, -14, -77, 49, 32, 32};
    message = new byte[]{50, 29, 76, -86, 9, 7, -24, -45, 33, -99, -29, 17, -122, 8, 98, 108};

    nonce = new byte[]{-117, -77, -114, -78, 5, -1, -111, 84, 91, 127, 15, 114, -42, -118, 12, 12};

    key = new byte[]{-83, -50, 75, -28, -55, -67, 77, -76, -27, -63, 77, -7, 71, -84, 89, 52};

    state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    cState =
        callProcess(
            new String[]{
                schwaemmCpath, "encrypt", String.valueOf(6), String.valueOf(16)
            });

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Schwaemm.encrypt(state, message);
    Assertions.assertThat(cState).isEqualTo(state);
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
        callProcess(new String[]{schwaemmCpath, "associate", String.valueOf(randomInt)});

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @Test
  void processAssociateDataHardcoded() throws IOException, InterruptedException {
    byte[] associate =
        new byte[]{
            75, 9, 44, 96, 54, -41, -82, -22, 45, -58, 2, 88, 4, 94, -121, 32, 114, -40, 35, -64,
            114,
            18, 111, 72, -50, 76, 43, -108, -63, 7, -90, -38
        };

    byte[] nonce = new byte[]{-16, 63, 29, 26, 85, -50, 1, 30, 69, -57, 69, 26, 13, 55, -57, -6};

    byte[] key =
        new byte[]{-46, -29, -126, -115, 64, -39, -56, 122, 74, -109, -47, 94, 103, -32, 51, 12};

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    int[] cState = callProcess(new String[]{schwaemmCpath, "associate", "32"});

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Assertions.assertThat(cState).isEqualTo(state);

    associate = new byte[]{73, 102, -74};
    nonce = new byte[]{85, -5, -5, 78, 120, -61, 89, 19, -15, -3, 116, 19, -79, 5, -98, -27};
    key = new byte[]{64, 67, -110, -123, 88, -65, 122, -59, 113, -87, 50, -59, -29, 1, -85, -77};

    state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    cState = callProcess(new String[]{schwaemmCpath, "associate", "3"});

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Assertions.assertThat(cState).isEqualTo(state);

    associate = new byte[]{24};
    nonce = new byte[]{-69, 114, 20, -36, 8, 34, -60, -49, -7, 71, -66, 73, -66, 81, -115, -96};
    key = new byte[]{-73, 19, 91, -7, 104, 124, -18, -94, 81, -34, 98, 11, -51, -110, -65, -18};

    state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    cState = callProcess(new String[]{schwaemmCpath, "associate", "1"});

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Assertions.assertThat(cState).isEqualTo(state);

    associate =
        new byte[]{
            101, -15, 127, 102, 29, 28, -65, -103, 101, 26, -107, 95, -38, 34, 13, -99, -93, 44, 1,
            -57, -43, 58, -70, -46, 121, -9, -123, 102
        };
    nonce = new byte[]{100, 17, -25, -61, 43, 45, 23, 62, 93, -116, 32, -39, 28, 82, 79, -126};
    key = new byte[]{-61, 68, 25, 124, 53, -61, -87, -56, -3, -128, 3, 36, 2, 83, 91, -92};

    state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    cState = callProcess(new String[]{schwaemmCpath, "associate", "28"});

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }
}
